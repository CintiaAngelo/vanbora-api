package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.ConversationMember;
import com.vanbora.api.modules.chat.repository.ConversationMemberRepository;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantém os monitores de cada conversa em sincronia com os vínculos reais:
 *
 * <ul>
 *   <li>Antes da contratação (sem matrícula ativa) a conversa é só Responsável ↔ Transportador.</li>
 *   <li>Com a contratação concluída entram os monitores ATIVOS escalados em algum percurso que
 *       leva um filho daquele responsável — nunca todos os funcionários do transportador.</li>
 *   <li>Quem perde o vínculo (removido, desativado, tirado do percurso, contrato encerrado) sai
 *       do grupo; o histórico de mensagens permanece.</li>
 * </ul>
 *
 * É recalculado por completo a partir do banco (idempotente), então pode ser chamado após
 * qualquer mudança de vínculo sem risco de acumular estado errado.
 */
@Service
public class ChatMembershipService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TripRepository tripRepository;
    private final TransporterProfileRepository transporterRepository;
    private final Clock clock;

    @Autowired
    public ChatMembershipService(ConversationRepository conversationRepository,
                                 ConversationMemberRepository memberRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 TripRepository tripRepository,
                                 TransporterProfileRepository transporterRepository) {
        this(conversationRepository, memberRepository, enrollmentRepository, tripRepository,
                transporterRepository, Clock.systemUTC());
    }

    ChatMembershipService(ConversationRepository conversationRepository,
                          ConversationMemberRepository memberRepository,
                          EnrollmentRepository enrollmentRepository,
                          TripRepository tripRepository,
                          TransporterProfileRepository transporterRepository,
                          Clock clock) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.tripRepository = tripRepository;
        this.transporterRepository = transporterRepository;
        this.clock = clock;
    }

    /** Recalcula os membros de todas as conversas do transportador. */
    @Transactional
    public void syncTransporter(Long transporterId) {
        TransporterProfile transporter = transporterRepository.findById(transporterId).orElse(null);
        if (transporter == null) {
            return;
        }
        Instant now = clock.instant();

        // Filhos com contrato ativo, agrupados por responsável.
        Map<Long, GuardianProfile> guardians = new LinkedHashMap<>();
        Map<Long, Set<Long>> dependentsByGuardian = new HashMap<>();
        for (Enrollment e : enrollmentRepository.findByTransporterIdAndActiveTrue(transporterId)) {
            GuardianProfile guardian = e.getDependent().getGuardian();
            guardians.putIfAbsent(guardian.getId(), guardian);
            dependentsByGuardian.computeIfAbsent(guardian.getId(), k -> new HashSet<>())
                    .add(e.getDependent().getId());
        }

        List<Trip> trips = tripRepository.findActiveByTransporter(transporterId);
        Set<Long> handled = new HashSet<>();

        // Contratação concluída: garante a conversa (o grupo existe mesmo sem conversa prévia).
        for (GuardianProfile guardian : guardians.values()) {
            Conversation conversation = conversationRepository
                    .findByGuardianIdAndTransporterId(guardian.getId(), transporterId)
                    .orElseGet(() -> {
                        Conversation created = new Conversation();
                        created.setGuardian(guardian);
                        created.setTransporter(transporter);
                        return conversationRepository.save(created);
                    });
            handled.add(conversation.getId());
            apply(conversation, staffFor(dependentsByGuardian.get(guardian.getId()), trips), now);
        }

        // Sem contrato ativo (negociação ou contrato encerrado): só as duas pontas.
        for (Conversation conversation : conversationRepository.findByTransporterId(transporterId)) {
            if (!handled.contains(conversation.getId())) {
                apply(conversation, Map.of(), now);
            }
        }
    }

    /** Monitores ativos escalados em percursos que levam algum destes dependentes. */
    private Map<Long, User> staffFor(Set<Long> dependentIds, List<Trip> trips) {
        Map<Long, User> staff = new LinkedHashMap<>();
        for (Trip trip : trips) {
            boolean servesFamily = dependentIds.stream().anyMatch(trip::includesDependent);
            if (!servesFamily) {
                continue;
            }
            for (Helper helper : trip.getMonitors()) {
                if (helper.hasActiveAccess()) {
                    staff.putIfAbsent(helper.getUser().getId(), helper.getUser());
                }
            }
        }
        return staff;
    }

    private void apply(Conversation conversation, Map<Long, User> desired, Instant now) {
        Set<Long> present = new LinkedHashSet<>();
        for (ConversationMember member : memberRepository.findByConversationId(conversation.getId())) {
            Long userId = member.getUser().getId();
            present.add(userId);
            boolean wanted = desired.containsKey(userId);
            if (wanted && !member.isCurrent()) {
                // Voltou a ter vínculo: entra de novo, vendo só as mensagens a partir de agora.
                member.setRemovedAt(null);
                member.setJoinedAt(now);
                member.setLastReadAt(null);
                memberRepository.save(member);
            } else if (!wanted && member.isCurrent()) {
                member.setRemovedAt(now);
                memberRepository.save(member);
            }
        }
        for (Map.Entry<Long, User> entry : desired.entrySet()) {
            if (!present.contains(entry.getKey())) {
                ConversationMember member = new ConversationMember();
                member.setConversation(conversation);
                member.setUser(entry.getValue());
                member.setJoinedAt(now);
                memberRepository.save(member);
            }
        }
    }
}
