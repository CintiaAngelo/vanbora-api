package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.user.domain.User;
import java.util.List;

/** Casos de uso de mensageria entre responsável e transportador. */
public interface ChatService {

    List<ConversationResponse> listConversations(User currentUser);

    /** Abre (ou reusa) a conversa do responsável autenticado com um transportador. */
    ConversationResponse startConversation(User guardianUser, Long transporterId);

    List<MessageResponse> listMessages(User currentUser, Long conversationId);

    MessageResponse sendMessage(User currentUser, Long conversationId, String text);

    /** Marca a conversa como lida pelo usuário (zera as não lidas dele). */
    void markRead(User currentUser, Long conversationId);
}
