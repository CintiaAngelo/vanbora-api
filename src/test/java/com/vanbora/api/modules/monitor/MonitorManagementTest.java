package com.vanbora.api.modules.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vanbora.api.modules.monitor.dto.MonitorAccessRequest;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.CreateHelperRequest;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import com.vanbora.api.modules.transporter.dto.UpdateHelperRequest;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.service.TransporterService;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.security.AppUserDetails;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.storage.StorageService;
import com.vanbora.api.support.JpaSliceTest;
import com.vanbora.api.support.TestFixtures;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Gestão de monitores pelo transportador: cadastro, edição, desativação e exclusão. */
@JpaSliceTest
class MonitorManagementTest {

    @MockBean StorageService storageService;
    @MockBean PushNotificationService pushNotificationService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired TransporterService transporterService;
    @Autowired TestFixtures fixtures;
    @Autowired UserRepository userRepository;
    @Autowired HelperRepository helperRepository;
    @Autowired TripRepository tripRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager em;

    private static MonitorAccessRequest access(String email) {
        return new MonitorAccessRequest(email, "(11) 98888-7777", "senha123");
    }

    @Test
    void transportadorCadastraMonitorComContaDePerfilMonitor() {
        TransporterProfile t = fixtures.transporter("Roberto");

        HelperResponse created = transporterService.addHelper(t.getUser().getId(),
                new CreateHelperRequest("João Silva", "Monitor", access("Joao@Exemplo.com ")));

        assertThat(created.hasAccess()).isTrue();
        assertThat(created.accessActive()).isTrue();
        assertThat(created.email()).isEqualTo("joao@exemplo.com");
        User account = userRepository.findByEmail("joao@exemplo.com").orElseThrow();
        assertThat(account.getRole()).isEqualTo(UserRole.MONITOR);
        assertThat(account.getName()).isEqualTo("João Silva");
        assertThat(passwordEncoder.matches("senha123", account.getPasswordHash())).isTrue();
    }

    @Test
    void acessoSoParaFuncaoMonitorESemEmailDuplicado() {
        TransporterProfile t = fixtures.transporter("Roberto");
        Long owner = t.getUser().getId();

        assertThatThrownBy(() -> transporterService.addHelper(owner,
                new CreateHelperRequest("Ana", "Auxiliar", access("ana@exemplo.com"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Monitor");

        transporterService.addHelper(owner, new CreateHelperRequest("Bia", "Monitora", access("bia@exemplo.com")));
        assertThatThrownBy(() -> transporterService.addHelper(owner,
                new CreateHelperRequest("Outra Bia", "Monitor", access("bia@exemplo.com"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void transportadorEditaDadosDoMonitor() {
        TransporterProfile t = fixtures.transporter("Roberto");
        Long owner = t.getUser().getId();
        HelperResponse created = transporterService.addHelper(owner,
                new CreateHelperRequest("João", "Monitor", access("joao2@exemplo.com")));

        transporterService.updateHelper(owner, created.id(), new UpdateHelperRequest("João Pedro", "Monitor", true));
        HelperResponse updated = transporterService.setHelperAccess(owner, created.id(),
                new MonitorAccessRequest("jp@exemplo.com", "(11) 97777-6666", null));

        assertThat(updated.email()).isEqualTo("jp@exemplo.com");
        assertThat(updated.phone()).isEqualTo("(11) 97777-6666");
        User account = userRepository.findByEmail("jp@exemplo.com").orElseThrow();
        assertThat(account.getName()).isEqualTo("João Pedro");
        // Senha em branco na edição mantém a atual.
        assertThat(passwordEncoder.matches("senha123", account.getPasswordHash())).isTrue();
    }

    @Test
    void desativarMonitorRevogaOAcessoNaHora() {
        TransporterProfile t = fixtures.transporter("Roberto");
        Long owner = t.getUser().getId();
        HelperResponse created = transporterService.addHelper(owner,
                new CreateHelperRequest("João", "Monitor", access("joao3@exemplo.com")));

        transporterService.updateHelper(owner, created.id(), new UpdateHelperRequest("João", "Monitor", false));

        User account = userRepository.findByEmail("joao3@exemplo.com").orElseThrow();
        assertThat(account.isActive()).isFalse();
        // O filtro JWT e o login usam isEnabled(): a conta desativada não autentica mais.
        assertThat(new AppUserDetails(account).isEnabled()).isFalse();

        // Reativar devolve o acesso.
        transporterService.updateHelper(owner, created.id(), new UpdateHelperRequest("João", "Monitor", true));
        assertThat(new AppUserDetails(userRepository.findById(account.getId()).orElseThrow()).isEnabled()).isTrue();
    }

    @Test
    void excluirMonitorPreservaAContaParaOHistoricoMasRevogaAcessoEEscala() {
        TransporterProfile t = fixtures.transporter("Roberto");
        Long owner = t.getUser().getId();
        HelperResponse created = transporterService.addHelper(owner,
                new CreateHelperRequest("João", "Monitor", access("joao4@exemplo.com")));
        Helper helper = helperRepository.findById(created.id()).orElseThrow();
        Long accountId = helper.getUser().getId();
        Trip trip = fixtures.trip(t, "Ida", List.of(helper), null);

        transporterService.deleteHelper(owner, created.id());
        em.flush();
        em.clear();

        assertThat(helperRepository.findById(created.id())).isEmpty();
        User account = userRepository.findById(accountId).orElseThrow(); // não foi apagada
        assertThat(account.isActive()).isFalse();
        assertThat(account.getName()).isEqualTo("João"); // histórico legível
        assertThat(account.getEmail()).doesNotContain("joao4"); // e-mail liberado/anonimizado
        assertThat(userRepository.findByEmail("joao4@exemplo.com")).isEmpty();
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getMonitors()).isEmpty();
    }

    @Test
    void transportadorNaoGerenciaMonitorDeOutroTransportador() {
        TransporterProfile dono = fixtures.transporter("Roberto");
        TransporterProfile outro = fixtures.transporter("Intruso");
        HelperResponse created = transporterService.addHelper(dono.getUser().getId(),
                new CreateHelperRequest("João", "Monitor", access("joao5@exemplo.com")));
        Long intruso = outro.getUser().getId();

        assertThatThrownBy(() -> transporterService.deleteHelper(intruso, created.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> transporterService.revokeHelperAccess(intruso, created.id()))
                .isInstanceOf(BusinessException.class);
        assertThat(userRepository.findByEmail("joao5@exemplo.com").orElseThrow().isActive()).isTrue();
    }

    @Test
    void perfilPublicoNaoExpoeContatoNemAcessoDoMonitor() {
        TransporterProfile t = fixtures.transporter("Roberto");
        transporterService.addHelper(t.getUser().getId(),
                new CreateHelperRequest("João", "Monitor", access("joao6@exemplo.com")));

        var detail = transporterService.getPublicProfile(t.getId());

        assertThat(detail.helpers()).singleElement().satisfies(h -> {
            assertThat(h.email()).isNull();
            assertThat(h.phone()).isNull();
            assertThat(h.hasAccess()).isFalse();
        });
    }
}
