package com.vanbora.api.modules.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.trip.domain.ChecklistItem;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse;
import com.vanbora.api.modules.trip.dto.TripRunResponse;
import com.vanbora.api.modules.trip.repository.SafetyChecklistRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.ChecklistStatus;
import com.vanbora.api.shared.storage.StorageService;
import com.vanbora.api.support.JpaSliceTest;
import com.vanbora.api.support.TestFixtures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fluxos que dependem de COMMIT de verdade (sem a transação de teste revertida): a
 * concorrência motorista × monitor e as notificações disparadas só após o commit.
 */
@JpaSliceTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SafetyChecklistCommittedFlowTest {

    private static final List<String> ALL_ITEMS =
            Arrays.stream(ChecklistItem.values()).map(Enum::name).toList();

    @MockBean StorageService storageService;
    @MockBean PushNotificationService pushNotificationService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired TestFixtures fixtures;
    @Autowired TripService tripService;
    @Autowired SafetyChecklistService checklistService;
    @Autowired SafetyChecklistRepository checklistRepository;

    TransporterProfile transporter;
    Helper joao;
    GuardianProfile mariana;
    GuardianProfile outraFamilia;
    Trip percurso;
    private final AtomicInteger keys = new AtomicInteger();
    private final List<String> deleted = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void cenario() {
        when(storageService.storePrivate(any(MultipartFile.class), anyString()))
                .thenAnswer(inv -> "checklists/foto-" + keys.incrementAndGet() + ".jpg");
        when(storageService.deletePrivate(anyString())).thenAnswer(inv -> deleted.add(inv.getArgument(0)));

        transporter = fixtures.transporter("Roberto");
        joao = fixtures.monitor(transporter, "João");
        mariana = fixtures.guardian("Mariana");
        outraFamilia = fixtures.guardian("Outra família");
        Dependent lucas = fixtures.dependent(mariana, "Lucas");
        Dependent enzo = fixtures.dependent(outraFamilia, "Enzo");
        fixtures.enroll(transporter, lucas);
        fixtures.enroll(transporter, enzo);
        percurso = fixtures.trip(transporter, "Ida — manhã", List.of(joao), List.of(lucas));
    }

    private static List<MultipartFile> photo() {
        return List.of(new MockMultipartFile("photos", "van.jpg", "image/jpeg", new byte[] {1, 2, 3}));
    }

    private Long finishedChecklist() {
        TripRunResponse run = tripService.start(transporter.getUser(), percurso.getId());
        return tripService.finish(transporter.getUser(), run.id()).checklistId();
    }

    @Test
    void motoristaEMonitorConcluindoJuntosGeramUmaUnicaConclusao() throws Exception {
        Long id = finishedChecklist();
        // Segura os dois envios até ambos terem gravado a foto: chegam juntos ao UPDATE.
        CountDownLatch bothUploaded = new CountDownLatch(2);
        when(storageService.storePrivate(any(MultipartFile.class), anyString())).thenAnswer(inv -> {
            bothUploaded.countDown();
            bothUploaded.await(5, TimeUnit.SECONDS);
            return "checklists/foto-" + keys.incrementAndGet() + ".jpg";
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<SafetyChecklistResponse>> results = pool.invokeAll(List.<Callable<SafetyChecklistResponse>>of(
                    () -> checklistService.complete(transporter.getUser(), id, ALL_ITEMS, photo()),
                    () -> checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo())));

            int successes = 0;
            List<Throwable> failures = new ArrayList<>();
            for (Future<SafetyChecklistResponse> result : results) {
                try {
                    result.get(10, TimeUnit.SECONDS);
                    successes++;
                } catch (java.util.concurrent.ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            assertThat(successes).isEqualTo(1);
            // No MySQL o perdedor recebe o 409 do UPDATE condicional; no H2 o conflito pode
            // aparecer como erro de concorrência do banco. Em ambos, nada dele é gravado.
            assertThat(failures).singleElement().satisfies(f -> assertThat(f)
                    .isInstanceOfAny(ChecklistAlreadyCompletedException.class,
                            org.springframework.dao.ConcurrencyFailureException.class,
                            org.springframework.dao.DataAccessException.class));
        } finally {
            pool.shutdownNow();
        }

        var stored = checklistRepository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ChecklistStatus.COMPLETED);
        SafetyChecklistResponse view = checklistService.get(transporter.getUser(), id);
        assertThat(view.photoCount()).isEqualTo(1); // só a foto do envio vencedor
        // O arquivo do envio perdedor foi apagado quando a transação dele foi revertida.
        assertThat(deleted).hasSize(1);
    }

    @Test
    void conclusaoConfirmadaNotificaOsResponsaveisDoPercurso() {
        Long id = finishedChecklist();

        // A equipe recebe o aviso de pendência depois do commit do "encerrar".
        verify(pushNotificationService, timeout(2000)).sendToUser(eq(joao.getUser().getId()),
                eq("⚠️ Verificação de segurança pendente"), anyString(), anyMap());

        checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo());

        verify(pushNotificationService, timeout(2000)).sendToUser(eq(mariana.getUser().getId()),
                eq("✅ Verificação de segurança concluída"), anyString(), anyMap());
        // Responsável de aluno que não está neste percurso não é notificado.
        User outro = outraFamilia.getUser();
        verify(pushNotificationService, never()).sendToUser(eq(outro.getId()),
                eq("✅ Verificação de segurança concluída"), anyString(), anyMap());
        assertThat(checklistRepository.findById(id).orElseThrow().getGuardiansNotified()).isEqualTo(1);
    }

    @Test
    void conclusaoRecusadaNaoNotificaNinguem() {
        Long id = finishedChecklist();

        try {
            checklistService.complete(joao.getUser(), id, ALL_ITEMS, List.of()); // sem foto
        } catch (RuntimeException expected) {
            // foto obrigatória
        }

        verify(pushNotificationService, never()).sendToUser(eq(mariana.getUser().getId()),
                anyString(), anyString(), anyMap());
        assertThat(checklistRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ChecklistStatus.PENDING);
    }
}
