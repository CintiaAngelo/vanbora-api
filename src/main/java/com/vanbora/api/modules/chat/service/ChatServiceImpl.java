package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.ConversationMember;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.dto.ChatBroadcast;
import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.ConversationResponse.ParticipantResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.chat.repository.ConversationMemberRepository;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.chat.repository.MessageRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.MessageType;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.storage.StorageService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementação da mensageria, com a perspectiva ("fromMe") do usuário autenticado.
 *
 * <p>Toda conversa tem duas pontas fixas (responsável e transportador). Depois da contratação,
 * monitores vinculados entram como {@link ConversationMember} (ver {@link ChatMembershipService})
 * e enxergam apenas as mensagens posteriores à sua entrada.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    static final String ROLE_GUARDIAN = "Responsável";
    static final String ROLE_TRANSPORTER = "Transportador";
    static final String ROLE_MONITOR = "Monitor";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GuardianProfileRepository guardianRepository;
    private final TransporterProfileRepository transporterRepository;
    private final StorageService storageService;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           ConversationMemberRepository memberRepository,
                           SimpMessagingTemplate messagingTemplate,
                           GuardianProfileRepository guardianRepository,
                           TransporterProfileRepository transporterRepository,
                           StorageService storageService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(User currentUser) {
        if (currentUser.getRole() == UserRole.MONITOR) {
            return memberRepository.findCurrentByUser(currentUser.getId()).stream()
                    .map(m -> toPreview(m.getConversation(), currentUser, m))
                    .toList();
        }
        return conversationsOf(currentUser).stream()
                .map(c -> toPreview(c, currentUser, null))
                .toList();
    }

    @Override
    @Transactional
    public ConversationResponse startConversation(User guardianUser, Long transporterId) {
        if (guardianUser.getRole() != UserRole.GUARDIAN) {
            throw new BusinessException("Apenas responsáveis podem iniciar conversas.");
        }
        GuardianProfile guardian = guardianRepository.findByUserId(guardianUser.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", guardianUser.getId()));
        TransporterProfile transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transportador", transporterId));

        Conversation conversation = conversationRepository
                .findByGuardianIdAndTransporterId(guardian.getId(), transporter.getId())
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setGuardian(guardian);
                    created.setTransporter(transporter);
                    return conversationRepository.save(created);
                });
        return toPreview(conversation, guardianUser, null);
    }

    @Override
    @Transactional
    public List<MessageResponse> listMessages(User currentUser, Long conversationId) {
        Access access = requireParticipant(currentUser, conversationId);
        Instant visibleFrom = access.member() != null ? access.member().getJoinedAt() : null;
        List<MessageResponse> result =
                messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                        .filter(m -> visibleFrom == null || !m.getSentAt().isBefore(visibleFrom))
                        .map(m -> toMessage(m, access.conversation(), currentUser))
                        .toList();
        // Abrir a conversa marca-a como lida para este usuário.
        touchLastRead(access, currentUser);
        return result;
    }

    @Override
    @Transactional
    public void markRead(User currentUser, Long conversationId) {
        touchLastRead(requireParticipant(currentUser, conversationId), currentUser);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(User currentUser, Long conversationId, String text) {
        Access access = requireParticipant(currentUser, conversationId);

        Message message = new Message();
        message.setConversation(access.conversation());
        message.setSender(currentUser);
        message.setText(text);
        message.setType(MessageType.TEXT);
        message.setSentAt(Instant.now());
        Message saved = messageRepository.save(message);
        broadcast(access.conversation(), saved, currentUser);
        return toMessage(saved, access.conversation(), currentUser);
    }

    @Override
    @Transactional
    public MessageResponse sendImage(User currentUser, Long conversationId, MultipartFile file) {
        Access access = requireParticipant(currentUser, conversationId);

        Message message = new Message();
        message.setConversation(access.conversation());
        message.setSender(currentUser);
        message.setText(""); // imagem não tem texto
        message.setType(MessageType.IMAGE);
        message.setMediaUrl(storageService.store(file, "chat"));
        message.setSentAt(Instant.now());
        Message saved = messageRepository.save(message);
        broadcast(access.conversation(), saved, currentUser);
        return toMessage(saved, access.conversation(), currentUser);
    }

    /** Difunde a mensagem para os assinantes do tópico da conversa (tempo real). */
    private void broadcast(Conversation conversation, Message saved, User sender) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getId(),
                new ChatBroadcast(
                        conversation.getId(),
                        saved.getId(),
                        saved.getText(),
                        TIME_FORMAT.format(saved.getSentAt()),
                        sender.getId(),
                        sender.getName(),
                        saved.typeOrDefault().name(),
                        saved.getMediaUrl(),
                        roleIn(conversation, sender)));
    }

    private List<Conversation> conversationsOf(User user) {
        if (user.getRole() == UserRole.GUARDIAN) {
            return conversationRepository.findByGuardianUserId(user.getId());
        }
        return conversationRepository.findByTransporterUserId(user.getId());
    }

    /** Conversa + (para monitores) a participação atual que dá direito de acesso. */
    private record Access(Conversation conversation, ConversationMember member) {
    }

    private Access requireParticipant(User user, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conversa", conversationId));

        if (user.getRole() == UserRole.MONITOR) {
            // Monitor: só com participação atual (vínculo vigente) e conta ativa.
            ConversationMember member = memberRepository
                    .findByConversationIdAndUserId(conversationId, user.getId())
                    .filter(ConversationMember::isCurrent)
                    .filter(m -> m.getUser().isActive())
                    .orElseThrow(() -> new BusinessException("Você não participa desta conversa."));
            return new Access(conversation, member);
        }

        boolean participant = user.getRole() == UserRole.GUARDIAN
                ? conversation.getGuardian().getUser().getId().equals(user.getId())
                : user.getRole() == UserRole.TRANSPORTER
                        && conversation.getTransporter().getUser().getId().equals(user.getId());

        if (!participant) {
            throw new BusinessException("Você não participa desta conversa.");
        }
        return new Access(conversation, null);
    }

    private ConversationResponse toPreview(Conversation conversation, User currentUser, ConversationMember me) {
        // Monitor vê o nome do responsável (a família atendida); as pontas veem uma à outra.
        String otherName = currentUser.getRole() == UserRole.GUARDIAN
                ? conversation.getTransporter().getUser().getName()
                : conversation.getGuardian().getUser().getName();

        Instant visibleFrom = me != null ? me.getJoinedAt() : null;
        List<Message> visible = conversation.getMessages().stream()
                .filter(m -> visibleFrom == null || !m.getSentAt().isBefore(visibleFrom))
                .toList();
        Message last = visible.isEmpty() ? null : visible.get(visible.size() - 1);
        String lastText = last == null ? ""
                : (last.typeOrDefault() == MessageType.IMAGE ? "📷 Foto" : last.getText());

        List<ConversationMember> staff = memberRepository.findCurrentByConversation(conversation.getId());
        return new ConversationResponse(
                conversation.getId(),
                otherName,
                lastText,
                last == null ? "" : TIME_FORMAT.format(last.getSentAt()),
                countUnread(visible, currentUser, lastReadOf(conversation, currentUser, me)),
                conversation.getTransporter().getId(),
                !staff.isEmpty(),
                participants(conversation, staff));
    }

    private List<ParticipantResponse> participants(Conversation conversation, List<ConversationMember> staff) {
        List<ParticipantResponse> result = new ArrayList<>();
        User guardian = conversation.getGuardian().getUser();
        User transporter = conversation.getTransporter().getUser();
        result.add(new ParticipantResponse(guardian.getId(), guardian.getName(), ROLE_GUARDIAN));
        result.add(new ParticipantResponse(transporter.getId(), transporter.getName(), ROLE_TRANSPORTER));
        for (ConversationMember member : staff) {
            if (member.getUser().isActive()) {
                result.add(new ParticipantResponse(
                        member.getUser().getId(), member.getUser().getName(), ROLE_MONITOR));
            }
        }
        return result;
    }

    /** Mensagens visíveis enviadas por OUTRA pessoa após a última leitura deste usuário. */
    private int countUnread(List<Message> visible, User currentUser, Instant lastRead) {
        return (int) visible.stream()
                .filter(m -> !m.getSender().getId().equals(currentUser.getId()))
                .filter(m -> lastRead == null || m.getSentAt().isAfter(lastRead))
                .count();
    }

    private Instant lastReadOf(Conversation conversation, User currentUser, ConversationMember me) {
        if (me != null) {
            return me.getLastReadAt();
        }
        return currentUser.getRole() == UserRole.GUARDIAN
                ? conversation.getGuardianLastReadAt()
                : conversation.getTransporterLastReadAt();
    }

    private void touchLastRead(Access access, User currentUser) {
        if (access.member() != null) {
            access.member().setLastReadAt(Instant.now());
            memberRepository.save(access.member());
            return;
        }
        Conversation conversation = access.conversation();
        if (currentUser.getRole() == UserRole.GUARDIAN) {
            conversation.setGuardianLastReadAt(Instant.now());
        } else {
            conversation.setTransporterLastReadAt(Instant.now());
        }
        conversationRepository.save(conversation);
    }

    /** Papel do autor NESTA conversa (o histórico continua legível mesmo de ex-monitores). */
    private String roleIn(Conversation conversation, User sender) {
        if (conversation.getGuardian().getUser().getId().equals(sender.getId())) {
            return ROLE_GUARDIAN;
        }
        if (conversation.getTransporter().getUser().getId().equals(sender.getId())) {
            return ROLE_TRANSPORTER;
        }
        return ROLE_MONITOR;
    }

    private MessageResponse toMessage(Message message, Conversation conversation, User currentUser) {
        return new MessageResponse(
                message.getId(),
                message.getText(),
                TIME_FORMAT.format(message.getSentAt()),
                message.getSender().getId().equals(currentUser.getId()),
                message.typeOrDefault().name(),
                message.getMediaUrl(),
                message.getSender().getName(),
                roleIn(conversation, message.getSender()));
    }
}
