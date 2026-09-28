package com.vanbora.api.modules.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.UpdateHelperRequest;
import com.vanbora.api.modules.transporter.service.TransporterService;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.dto.TripRequest;
import com.vanbora.api.modules.trip.service.TripService;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.storage.StorageService;
import com.vanbora.api.support.JpaSliceTest;
import com.vanbora.api.support.TestFixtures;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Chat: durante a contratação só Responsável ↔ Transportador; após a contratação, os monitores
 * dos percursos do aluno entram automaticamente; quem perde o vínculo sai, e o histórico fica.
 */
@JpaSliceTest
class ChatGroupTest {

    @MockBean StorageService storageService;
    @MockBean PushNotificationService pushNotificationService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired TestFixtures fixtures;
    @Autowired ChatService chatService;
    @Autowired ChatMembershipService membershipService;
    @Autowired TripService tripService;
    @Autowired TransporterService transporterService;
    @Autowired ConversationRepository conversationRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired EntityManager em;

    TransporterProfile transporter;
    GuardianProfile mariana;
    Dependent lucas;
    Dependent sofia;
    Helper joao;
    Helper maria;
    Trip percursoLucas;
    Conversation conversa;

    @BeforeEach
    void cenario() {
        transporter = fixtures.transporter("Roberto");
        mariana = fixtures.guardian("Mariana");
        lucas = fixtures.dependent(mariana, "Lucas");
        sofia = fixtures.dependent(fixtures.guardian("Juliana"), "Sofia");
        joao = fixtures.monitor(transporter, "João");
        maria = fixtures.monitor(transporter, "Maria");
        percursoLucas = fixtures.trip(transporter, "Ida", List.of(joao), List.of(lucas));
        fixtures.trip(transporter, "Outro", List.of(maria), List.of(sofia));
        // Negociação: a responsável abre a conversa antes de contratar.
        Long conversationId = chatService.startConversation(mariana.getUser(), transporter.getId()).id();
        conversa = conversationRepository.findById(conversationId).orElseThrow();
    }

    private void contratar() {
        fixtures.enroll(transporter, lucas);
        membershipService.syncTransporter(transporter.getId());
    }

    @Test
    void duranteAContratacaoOChatESoResponsavelETransportador() {
        chatService.sendMessage(mariana.getUser(), conversa.getId(), "Qual o valor para o Lucas?");
        membershipService.syncTransporter(transporter.getId());

        ConversationResponse view = chatService.listConversations(mariana.getUser()).get(0);
        assertThat(view.group()).isFalse();
        assertThat(view.participants()).extracting(ConversationResponse.ParticipantResponse::role)
                .containsExactly("Responsável", "Transportador");
        assertThat(chatService.listConversations(joao.getUser())).isEmpty();
        assertThatThrownBy(() -> chatService.listMessages(joao.getUser(), conversa.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void aposAContratacaoEntramSoOsMonitoresDoPercursoDoAluno() {
        contratar();

        ConversationResponse view = chatService.listConversations(mariana.getUser()).get(0);
        assertThat(view.group()).isTrue();
        assertThat(view.participants()).extracting(ConversationResponse.ParticipantResponse::name)
                .containsExactly("Mariana", "Roberto", "João");
        assertThat(chatService.listConversations(joao.getUser())).hasSize(1);
        // Maria é do transportador, mas não trabalha no percurso do Lucas.
        assertThat(chatService.listConversations(maria.getUser())).isEmpty();
    }

    @Test
    void monitorNaoVeANegociacaoAnteriorMasVeEEnviaAsMensagensNovas() throws InterruptedException {
        chatService.sendMessage(mariana.getUser(), conversa.getId(), "Proposta: R$ 350");
        Thread.sleep(5); // garante sentAt anterior à entrada do monitor
        contratar();
        Thread.sleep(5);
        chatService.sendMessage(mariana.getUser(), conversa.getId(), "Bom dia, equipe!");
        chatService.sendMessage(joao.getUser(), conversa.getId(), "Bom dia! Lucas embarcou.");

        List<MessageResponse> doMonitor = chatService.listMessages(joao.getUser(), conversa.getId());
        assertThat(doMonitor).extracting(MessageResponse::text)
                .containsExactly("Bom dia, equipe!", "Bom dia! Lucas embarcou.");

        List<MessageResponse> daResponsavel = chatService.listMessages(mariana.getUser(), conversa.getId());
        assertThat(daResponsavel).extracting(MessageResponse::text)
                .containsExactly("Proposta: R$ 350", "Bom dia, equipe!", "Bom dia! Lucas embarcou.");
        assertThat(daResponsavel.get(2).senderName()).isEqualTo("João");
        assertThat(daResponsavel.get(2).senderRole()).isEqualTo("Monitor");
    }

    @Test
    void monitorTiradoDoPercursoSaiDoGrupoEOHistoricoPermanece() {
        contratar();
        chatService.sendMessage(joao.getUser(), conversa.getId(), "Chegamos na escola.");

        tripService.update(transporter.getUser(), percursoLucas.getId(),
                new TripRequest("Ida", List.of(), false, List.of(lucas.getId())));

        assertThat(chatService.listConversations(joao.getUser())).isEmpty();
        assertThatThrownBy(() -> chatService.sendMessage(joao.getUser(), conversa.getId(), "oi"))
                .isInstanceOf(BusinessException.class);
        assertThat(chatService.listMessages(mariana.getUser(), conversa.getId()))
                .extracting(MessageResponse::text).containsExactly("Chegamos na escola.");
    }

    @Test
    void monitorDesativadoOuRemovidoPerdeOAcessoAoGrupo() {
        contratar();
        Long joaoUserId = joao.getUser().getId();
        transporterService.updateHelper(transporter.getUser().getId(), joao.getId(),
                new UpdateHelperRequest("João", "Monitor", false));

        assertThat(conversationRepository.existsByIdAndParticipant(conversa.getId(), joaoUserId))
                .isFalse(); // WebSocket: SUBSCRIBE e entregas passam a ser recusados
        assertThat(chatService.listConversations(mariana.getUser()).get(0).group()).isFalse();

        transporterService.updateHelper(transporter.getUser().getId(), joao.getId(),
                new UpdateHelperRequest("João", "Monitor", true));
        assertThat(conversationRepository.existsByIdAndParticipant(conversa.getId(), joaoUserId))
                .isTrue();

        transporterService.deleteHelper(transporter.getUser().getId(), joao.getId());
        assertThat(conversationRepository.existsByIdAndParticipant(conversa.getId(), joaoUserId))
                .isFalse();
    }

    @Test
    void contratoEncerradoTiraOsMonitores() {
        contratar();
        Enrollment enrollment = enrollmentRepository
                .findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(lucas.getId(), mariana.getId())
                .orElseThrow();
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);
        membershipService.syncTransporter(transporter.getId());

        assertThat(chatService.listConversations(joao.getUser())).isEmpty();
        assertThat(chatService.listConversations(mariana.getUser()).get(0).group()).isFalse();
    }

    @Test
    void contratacaoSemConversaPreviaCriaOGrupo() {
        GuardianProfile nova = fixtures.guardian("Nova família");
        Dependent theo = fixtures.dependent(nova, "Théo");
        fixtures.enroll(transporter, theo);

        membershipService.syncTransporter(transporter.getId()); // Ida leva só o Lucas: Théo sem monitor
        assertThat(chatService.listConversations(nova.getUser())).singleElement()
                .satisfies(c -> assertThat(c.group()).isFalse());

        tripService.update(transporter.getUser(), percursoLucas.getId(),
                new TripRequest("Ida", List.of(joao.getId()), true, List.of()));
        assertThat(chatService.listConversations(nova.getUser())).singleElement()
                .satisfies(c -> assertThat(c.participants()).extracting(p -> p.name()).contains("João"));
    }
}
