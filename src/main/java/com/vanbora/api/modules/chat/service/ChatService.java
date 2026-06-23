package com.vanbora.api.modules.chat.service;

import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.user.domain.User;
import java.util.List;

/** Casos de uso de mensageria entre responsável e transportador. */
public interface ChatService {

    List<ConversationResponse> listConversations(User currentUser);

    List<MessageResponse> listMessages(User currentUser, Long conversationId);

    MessageResponse sendMessage(User currentUser, Long conversationId, String text);
}
