package com.vanbora.api.modules.chat.dto;

/** Resumo de conversa para a lista de chats. */
public record ConversationResponse(
        Long id,
        String name,
        String lastMessage,
        String time,
        int unread
) {
}
