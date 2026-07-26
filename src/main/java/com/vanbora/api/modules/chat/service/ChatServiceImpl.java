package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.dto.ChatBroadcast;
import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
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
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementação da mensageria, com a perspectiva ("fromMe") do usuário autenticado. */
@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GuardianProfileRepository guardianRepository;
    private final TransporterProfileRepository transporterRepository;
    private final StorageService storageService;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           SimpMessagingTemplate messagingTemplate,
                           GuardianProfileRepository guardianRepository,
                           TransporterProfileRepository transporterRepository,
                           StorageService storageService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(User currentUser) {
        return conversationsOf(currentUser).stream()
                .map(c -> toPreview(c, currentUser))
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
        return toPreview(conversation, guardianUser);
    }

    @Override
    @Transactional
    public List<MessageResponse> listMessages(User currentUser, Long conversationId) {
        Conversation conversation = requireParticipant(currentUser, conversationId);
        List<MessageResponse> result =
                messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                        .map(m -> toMessage(m, currentUser))
                        .toList();
        // Abrir a conversa marca-a como lida para este usuário.
        touchLastRead(conversation, currentUser);
        return result;
    }

    @Override
    @Transactional
    public void markRead(User currentUser, Long conversationId) {
        Conversation conversation = requireParticipant(currentUser, conversationId);
        touchLastRead(conversation, currentUser);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(User currentUser, Long conversationId, String text) {
        Conversation conversation = requireParticipant(currentUser, conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setText(text);
        message.setType(MessageType.TEXT);
        message.setSentAt(Instant.now());
        Message saved = messageRepository.save(message);
        broadcast(conversationId, saved, currentUser);
        return toMessage(saved, currentUser);
    }

    @Override
    @Transactional
    public MessageResponse sendImage(User currentUser, Long conversationId, MultipartFile file) {
        Conversation conversation = requireParticipant(currentUser, conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setText(""); // imagem não tem texto
        message.setType(MessageType.IMAGE);
        message.setMediaUrl(storageService.store(file, "chat"));
        message.setSentAt(Instant.now());
        Message saved = messageRepository.save(message);
        broadcast(conversationId, saved, currentUser);
        return toMessage(saved, currentUser);
    }

    /** Difunde a mensagem para os assinantes do tópico da conversa (tempo real). */
    private void broadcast(Long conversationId, Message saved, User sender) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                new ChatBroadcast(
                        conversationId,
                        saved.getId(),
                        saved.getText(),
                        TIME_FORMAT.format(saved.getSentAt()),
                        sender.getId(),
                        sender.getName(),
                        saved.typeOrDefault().name(),
                        saved.getMediaUrl()));
    }

    private List<Conversation> conversationsOf(User user) {
        if (user.getRole() == UserRole.GUARDIAN) {
            return conversationRepository.findByGuardianUserId(user.getId());
        }
        return conversationRepository.findByTransporterUserId(user.getId());
    }

    private Conversation requireParticipant(User user, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conversa", conversationId));

        boolean participant = user.getRole() == UserRole.GUARDIAN
                ? conversation.getGuardian().getUser().getId().equals(user.getId())
                : conversation.getTransporter().getUser().getId().equals(user.getId());

        if (!participant) {
            throw new BusinessException("Você não participa desta conversa.");
        }
        return conversation;
    }

    private ConversationResponse toPreview(Conversation conversation, User currentUser) {
        String otherName = currentUser.getRole() == UserRole.GUARDIAN
                ? conversation.getTransporter().getUser().getName()
                : conversation.getGuardian().getUser().getName();

        List<Message> messages = conversation.getMessages();
        Message last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        String lastText = last == null ? ""
                : (last.typeOrDefault() == MessageType.IMAGE ? "📷 Foto" : last.getText());

        return new ConversationResponse(
                conversation.getId(),
                otherName,
                lastText,
                last == null ? "" : TIME_FORMAT.format(last.getSentAt()),
                countUnread(conversation, currentUser),
                conversation.getTransporter().getId());
    }

    /** Mensagens enviadas pelo OUTRO lado após a última leitura deste usuário. */
    private int countUnread(Conversation conversation, User currentUser) {
        Instant lastRead = lastReadOf(conversation, currentUser);
        return (int) conversation.getMessages().stream()
                .filter(m -> !m.getSender().getId().equals(currentUser.getId()))
                .filter(m -> lastRead == null || m.getSentAt().isAfter(lastRead))
                .count();
    }

    private Instant lastReadOf(Conversation conversation, User currentUser) {
        return currentUser.getRole() == UserRole.GUARDIAN
                ? conversation.getGuardianLastReadAt()
                : conversation.getTransporterLastReadAt();
    }

    private void touchLastRead(Conversation conversation, User currentUser) {
        if (currentUser.getRole() == UserRole.GUARDIAN) {
            conversation.setGuardianLastReadAt(Instant.now());
        } else {
            conversation.setTransporterLastReadAt(Instant.now());
        }
        conversationRepository.save(conversation);
    }

    private MessageResponse toMessage(Message message, User currentUser) {
        return new MessageResponse(
                message.getId(),
                message.getText(),
                TIME_FORMAT.format(message.getSentAt()),
                message.getSender().getId().equals(currentUser.getId()),
                message.typeOrDefault().name(),
                message.getMediaUrl());
    }
}
