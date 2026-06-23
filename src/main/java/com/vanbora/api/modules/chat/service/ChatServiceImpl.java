package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.chat.repository.MessageRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação da mensageria, com a perspectiva ("fromMe") do usuário autenticado. */
@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(User currentUser) {
        return conversationsOf(currentUser).stream()
                .map(c -> toPreview(c, currentUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(User currentUser, Long conversationId) {
        requireParticipant(currentUser, conversationId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(m -> toMessage(m, currentUser))
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(User currentUser, Long conversationId, String text) {
        Conversation conversation = requireParticipant(currentUser, conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setText(text);
        message.setSentAt(Instant.now());

        return toMessage(messageRepository.save(message), currentUser);
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

        return new ConversationResponse(
                conversation.getId(),
                otherName,
                last == null ? "" : last.getText(),
                last == null ? "" : TIME_FORMAT.format(last.getSentAt()),
                0);
    }

    private MessageResponse toMessage(Message message, User currentUser) {
        return new MessageResponse(
                message.getId(),
                message.getText(),
                TIME_FORMAT.format(message.getSentAt()),
                message.getSender().getId().equals(currentUser.getId()));
    }
}
