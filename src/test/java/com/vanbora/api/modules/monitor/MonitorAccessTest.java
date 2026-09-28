package com.vanbora.api.modules.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.monitor.dto.MonitorStudentResponse;
import com.vanbora.api.modules.monitor.service.MonitorService;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.dto.TripRequest;
import com.vanbora.api.modules.trip.dto.TripResponse;
import com.vanbora.api.modules.trip.service.TripService;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.storage.StorageService;
import com.vanbora.api.support.JpaSliceTest;
import com.vanbora.api.support.TestFixtures;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

/**
 * Permissões do monitor: o acesso nasce da escala real (Transportador → Percurso → Monitor →
 * Alunos), nunca do simples vínculo com o transportador, e não é burlável trocando ids.
 */
@JpaSliceTest
class MonitorAccessTest {

    @MockBean StorageService storageService;
    @MockBean PushNotificationService pushNotificationService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired TestFixtures fixtures;
    @Autowired MonitorService monitorService;
    @Autowired TripService tripService;

    TransporterProfile transporter;
    Helper joao;
    Helper maria;
    Helper pedro;
    Dependent lucas;
    Dependent sofia;
    Trip percurso1;
    Trip percurso2;

    @BeforeEach
    void cenario() {
        // Transportador A com João, Maria e Pedro; Percurso 1 → João (Lucas), Percurso 2 → Maria (Sofia).
        transporter = fixtures.transporter("Transportador A");
        joao = fixtures.monitor(transporter, "João");
        maria = fixtures.monitor(transporter, "Maria");
        pedro = fixtures.monitor(transporter, "Pedro");
        GuardianProfile mariana = fixtures.guardian("Mariana");
        lucas = fixtures.dependent(mariana, "Lucas");
        sofia = fixtures.dependent(fixtures.guardian("Juliana"), "Sofia");
        fixtures.enroll(transporter, lucas);
        fixtures.enroll(transporter, sofia);
        percurso1 = fixtures.trip(transporter, "Percurso 1", List.of(joao), List.of(lucas));
        percurso2 = fixtures.trip(transporter, "Percurso 2", List.of(maria), List.of(sofia));
    }

    @Test
    void monitorVeSomenteOsAlunosDosSeusPercursos() {
        assertThat(monitorService.students(joao.getUser()))
                .extracting(MonitorStudentResponse::name).containsExactly("Lucas");
        assertThat(monitorService.students(maria.getUser()))
                .extracting(MonitorStudentResponse::name).containsExactly("Sofia");
    }

    @Test
    void monitorSemPercursoNaoVeNenhumAluno() {
        assertThat(monitorService.students(pedro.getUser())).isEmpty();
        assertThat(tripService.list(pedro.getUser())).isEmpty();
    }

    @Test
    void monitorNaoRecebeAutomaticamenteAcessoAOutroPercurso() {
        assertThat(tripService.list(joao.getUser())).extracting(TripResponse::name).containsExactly("Percurso 1");
        // Trocar o id na requisição não adianta: o percurso alheio responde como inexistente.
        assertThatThrownBy(() -> tripService.start(joao.getUser(), percurso2.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void isolamentoEntreTransportadores() {
        TransporterProfile outro = fixtures.transporter("Transportador B");
        Dependent aluno = fixtures.dependent(fixtures.guardian("Carla"), "Enzo");
        fixtures.enroll(outro, aluno);
        Trip alheio = fixtures.trip(outro, "Percurso B", List.of(), null);

        assertThatThrownBy(() -> tripService.start(joao.getUser(), alheio.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(monitorService.students(joao.getUser()))
                .extracting(MonitorStudentResponse::name).doesNotContain("Enzo");
        // Transportador também não opera percurso de outro transportador.
        assertThatThrownBy(() -> tripService.update(transporter.getUser(), alheio.getId(),
                new TripRequest("x", List.of(), true, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        // Nem escala monitor alheio / aluno alheio num percurso próprio.
        assertThatThrownBy(() -> tripService.create(outro.getUser(),
                new TripRequest("Roubo", List.of(joao.getId()), true, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> tripService.create(outro.getUser(),
                new TripRequest("Roubo", List.of(), false, List.of(lucas.getId()))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void monitorRemovidoDoPercursoPerdeAcessoAosDadosDele() {
        tripService.update(transporter.getUser(), percurso1.getId(),
                new TripRequest("Percurso 1", List.of(), false, List.of(lucas.getId())));

        assertThat(monitorService.students(joao.getUser())).isEmpty();
        assertThatThrownBy(() -> tripService.start(joao.getUser(), percurso1.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void monitorDesativadoNaoAcessaNada() {
        joao.setActive(false);

        assertThatThrownBy(() -> monitorService.students(joao.getUser())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tripService.list(joao.getUser())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void responsavelNaoOperaPercursos() {
        GuardianProfile guardian = fixtures.guardian("Curiosa");
        assertThatThrownBy(() -> tripService.list(guardian.getUser())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void monitorNaoGerenciaPercursos() {
        assertThatThrownBy(() -> tripService.create(joao.getUser(),
                new TripRequest("Meu", List.of(), true, List.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listaDoMonitorNaoTemDadosFinanceiros() {
        assertThat(Arrays.stream(MonitorStudentResponse.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("financeStatus", "monthlyFee");
    }
}
