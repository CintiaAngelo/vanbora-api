package com.vanbora.api.modules.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.trip.domain.ChecklistItem;
import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse;
import com.vanbora.api.modules.trip.dto.TripRunResponse;
import com.vanbora.api.modules.trip.repository.SafetyChecklistPhotoRepository;
import com.vanbora.api.modules.trip.repository.SafetyChecklistRepository;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.shared.enums.ChecklistStatus;
import com.vanbora.api.shared.enums.TripRunStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.storage.StorageService;
import com.vanbora.api.support.JpaSliceTest;
import com.vanbora.api.support.TestFixtures;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/** Checklist de segurança pós-percurso: criação, visualização, conclusão, foto, retenção e aviso. */
@JpaSliceTest
class SafetyChecklistTest {

    private static final List<String> ALL_ITEMS =
            Arrays.stream(ChecklistItem.values()).map(Enum::name).toList();

    @MockBean StorageService storageService;
    @MockBean PushNotificationService pushNotificationService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired TestFixtures fixtures;
    @Autowired TripService tripService;
    @Autowired SafetyChecklistService checklistService;
    @Autowired SafetyChecklistRepository checklistRepository;
    @Autowired SafetyChecklistPhotoRepository photoRepository;
    @Autowired TripRepository tripRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired GuardianProfileRepository guardianRepository;
    @Autowired DependentRepository dependentRepository;
    @Autowired TripAccessService access;
    @Autowired ChecklistPhotoUrlSigner urlSigner;
    @Autowired ApplicationEventPublisher events;
    @Autowired EntityManager em;

    TransporterProfile transporter;
    Helper joao;
    Helper maria;
    GuardianProfile mariana;
    GuardianProfile juliana;
    Trip percurso;
    private final AtomicInteger keys = new AtomicInteger();

    @BeforeEach
    void cenario() {
        when(storageService.storePrivate(any(MultipartFile.class), anyString()))
                .thenAnswer(inv -> "checklists/unit-" + keys.incrementAndGet() + ".jpg");
        when(storageService.deletePrivate(anyString())).thenReturn(true);

        transporter = fixtures.transporter("Roberto");
        joao = fixtures.monitor(transporter, "João");
        maria = fixtures.monitor(transporter, "Maria");
        mariana = fixtures.guardian("Mariana");
        juliana = fixtures.guardian("Juliana");
        Dependent lucas = fixtures.dependent(mariana, "Lucas");
        Dependent sofia = fixtures.dependent(juliana, "Sofia");
        fixtures.enroll(transporter, lucas);
        fixtures.enroll(transporter, sofia);
        percurso = fixtures.trip(transporter, "Ida — manhã", List.of(joao), List.of(lucas));
        // Maria está em outro percurso (sem acesso a este).
        fixtures.trip(transporter, "Volta — tarde", List.of(maria), List.of(sofia));
    }

    private static List<MultipartFile> photo() {
        return List.of(new MockMultipartFile("photos", "van.jpg", "image/jpeg", new byte[] {1, 2, 3}));
    }

    /** Inicia e encerra o percurso, devolvendo o checklist aberto. */
    private Long finishedChecklist() {
        TripRunResponse run = tripService.start(transporter.getUser(), percurso.getId());
        TripRunResponse finished = tripService.finish(transporter.getUser(), run.id());
        return finished.checklistId();
    }

    @Test
    void encerrarPercursoAbreChecklistPendenteParaMotoristaEMonitor() {
        Long checklistId = finishedChecklist();

        SafetyChecklistResponse doMotorista = checklistService.get(transporter.getUser(), checklistId);
        SafetyChecklistResponse doMonitor = checklistService.get(joao.getUser(), checklistId);
        assertThat(doMotorista.status()).isEqualTo(ChecklistStatus.PENDING);
        assertThat(doMonitor.id()).isEqualTo(doMotorista.id()); // o MESMO checklist
        assertThat(checklistService.list(joao.getUser(), ChecklistStatus.PENDING))
                .extracting(SafetyChecklistResponse::id).containsExactly(checklistId);
        assertThat(checklistService.list(transporter.getUser(), ChecklistStatus.PENDING))
                .extracting(SafetyChecklistResponse::id).containsExactly(checklistId);
    }

    @Test
    void encerrarDuasVezesNaoDuplicaChecklist() {
        TripRunResponse run = tripService.start(transporter.getUser(), percurso.getId());
        TripRunResponse first = tripService.finish(transporter.getUser(), run.id());
        TripRunResponse second = tripService.finish(joao.getUser(), run.id());

        assertThat(second.checklistId()).isEqualTo(first.checklistId());
        assertThat(checklistRepository.findForTransporter(transporter.getId(), null)).hasSize(1);
    }

    @Test
    void percursoCanceladoNaoGeraChecklist() {
        TripRunResponse run = tripService.start(transporter.getUser(), percurso.getId());
        TripRunResponse cancelled = tripService.cancel(joao.getUser(), run.id());

        assertThat(cancelled.status()).isEqualTo(TripRunStatus.CANCELLED);
        assertThat(checklistRepository.findByRunId(run.id())).isEmpty();
        assertThatThrownBy(() -> tripService.finish(transporter.getUser(), run.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void motoristaConcluiSozinhoEmPercursoSemMonitor() {
        Trip semMonitor = fixtures.trip(transporter, "Extra", List.of(), null);
        TripRunResponse run = tripService.start(transporter.getUser(), semMonitor.getId());
        Long id = tripService.finish(transporter.getUser(), run.id()).checklistId();

        SafetyChecklistResponse done = checklistService.complete(transporter.getUser(), id, ALL_ITEMS, photo());

        assertThat(done.status()).isEqualTo(ChecklistStatus.COMPLETED);
        assertThat(done.completedByRole()).isEqualTo("Motorista");
        assertThat(done.completedByName()).isEqualTo("Roberto");
        assertThat(done.completedByMe()).isTrue();
    }

    @Test
    void monitorConcluiEOMotoristaVeQuemRealizouEQuando() {
        Long id = finishedChecklist();

        SafetyChecklistResponse done = checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo());
        assertThat(done.completedByRole()).isEqualTo("Monitor");

        SafetyChecklistResponse visto = checklistService.get(transporter.getUser(), id);
        assertThat(visto.status()).isEqualTo(ChecklistStatus.COMPLETED);
        assertThat(visto.completedByName()).isEqualTo("João");
        assertThat(visto.completedByRole()).isEqualTo("Monitor");
        assertThat(visto.completedAt()).isNotNull();
        assertThat(visto.completedByMe()).isFalse();
        assertThat(visto.items()).allMatch(SafetyChecklistResponse.ChecklistItemResponse::confirmed);
        assertThat(visto.photos()).singleElement().satisfies(p -> assertThat(p.url()).contains("sig="));
        // Deixa de ser pendente para o outro profissional, mas continua no histórico.
        assertThat(checklistService.list(transporter.getUser(), ChecklistStatus.PENDING)).isEmpty();
        assertThat(checklistService.list(transporter.getUser(), ChecklistStatus.COMPLETED)).hasSize(1);
    }

    @Test
    void segundoEnvioRecebe409ComQuemConcluiuENaoGravaNada() {
        Long id = finishedChecklist();
        checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo());

        assertThatThrownBy(() -> checklistService.complete(transporter.getUser(), id, ALL_ITEMS, photo()))
                .isInstanceOf(ChecklistAlreadyCompletedException.class)
                .hasMessageContaining("João — Monitor");
        em.flush();
        em.clear();
        SafetyChecklist stored = checklistRepository.findById(id).orElseThrow();
        assertThat(stored.getCompletedByName()).isEqualTo("João");
        assertThat(stored.getPhotos()).hasSize(1); // a foto do segundo envio não entrou
    }

    @Test
    void fotoEItensSaoObrigatorios() {
        Long id = finishedChecklist();

        assertThatThrownBy(() -> checklistService.complete(joao.getUser(), id, ALL_ITEMS, List.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("foto");
        assertThatThrownBy(() -> checklistService.complete(joao.getUser(), id, ALL_ITEMS, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("foto");
        assertThatThrownBy(() -> checklistService.complete(joao.getUser(), id,
                List.of(ChecklistItem.NO_CHILD_LEFT.name()), photo()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("todos os itens");

        assertThat(checklistRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ChecklistStatus.PENDING);
    }

    @Test
    void falhaNoUploadNaoConcluiOChecklist() {
        Long id = finishedChecklist();
        when(storageService.storePrivate(any(MultipartFile.class), anyString()))
                .thenThrow(new BusinessException("Falha ao salvar a imagem."));

        assertThatThrownBy(() -> checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo()))
                .isInstanceOf(BusinessException.class);
        assertThat(checklistRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ChecklistStatus.PENDING);
    }

    @Test
    void monitorDeOutroPercursoNaoVeNemConcluiOChecklist() {
        Long id = finishedChecklist();

        assertThatThrownBy(() -> checklistService.get(maria.getUser(), id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> checklistService.complete(maria.getUser(), id, ALL_ITEMS, photo()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(checklistService.list(maria.getUser(), null)).isEmpty();
    }

    @Test
    void retencaoApagaSoAsFotosDepoisDe15DiasEPreservaOHistorico() {
        Long id = finishedChecklist();
        checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo());
        em.flush();
        em.clear();
        // A limpeza por rollback do teste anterior (arquivos de transações revertidas) pode
        // cair neste mock; zera para contar só a retenção.
        clearInvocations(storageService);

        SafetyChecklistServiceImpl em14Dias = serviceAt(Instant.now().plus(Duration.ofDays(14)));
        assertThat(em14Dias.purgeExpiredPhotos()).isZero();

        SafetyChecklistServiceImpl em16Dias = serviceAt(Instant.now().plus(Duration.ofDays(16)));
        // >= 1: o H2 é compartilhado com testes que confirmam transações (fotos deles também vencem).
        assertThat(em16Dias.purgeExpiredPhotos()).isGreaterThanOrEqualTo(1);
        verify(storageService).deletePrivate("checklists/unit-" + keys.get() + ".jpg");
        // Idempotente: rodar de novo não faz nada.
        assertThat(em16Dias.purgeExpiredPhotos()).isZero();
        em.flush();
        em.clear();

        SafetyChecklist stored = checklistRepository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ChecklistStatus.COMPLETED);
        assertThat(stored.getCompletedByName()).isEqualTo("João");
        assertThat(stored.getCompletedByRole()).isEqualTo("Monitor");
        assertThat(stored.getCompletedAt()).isNotNull();
        assertThat(stored.getPhotosPurgedAt()).isNotNull();
        assertThat(stored.getPhotos()).singleElement().satisfies(p -> {
            assertThat(p.getStorageKey()).isNull();
            assertThat(p.getPurgedAt()).isNotNull();
            assertThat(p.getUploadedBy().getName()).isEqualTo("João");
        });
        SafetyChecklistResponse view = checklistService.get(transporter.getUser(), id);
        assertThat(view.photos()).singleElement().satisfies(p -> {
            assertThat(p.available()).isFalse();
            assertThat(p.url()).isNull();
        });
    }

    @Test
    void responsavelVeAConferenciaDoPercursoDoFilho() {
        Long id = finishedChecklist();
        checklistService.complete(joao.getUser(), id, ALL_ITEMS, photo());

        assertThat(checklistService.recentForGuardian(mariana.getUser(), null))
                .singleElement().satisfies(c -> {
                    assertThat(c.tripName()).isEqualTo("Ida — manhã");
                    assertThat(c.completedByName()).isEqualTo("João");
                });
        // Juliana (filha em outro percurso) não recebe a conferência deste percurso.
        assertThat(checklistService.recentForGuardian(juliana.getUser(), null)).isEmpty();
    }

    private SafetyChecklistServiceImpl serviceAt(Instant instant) {
        return new SafetyChecklistServiceImpl(checklistRepository, photoRepository, tripRepository,
                enrollmentRepository, guardianRepository, dependentRepository, access, storageService,
                urlSigner, events, 15, 5, Clock.fixed(instant, ZoneOffset.UTC));
    }
}
