package com.vanbora.api.modules.chat.dto;

/**
 * Mensagem difundida pelo WebSocket para os participantes da conversa. É
 * NEUTRA (sem {@code fromMe}, que é relativo a cada lado) — cada cliente calcula
 * {@code fromMe} comparando {@code senderUserId} com o próprio id.
 */
public record ChatBroadcast(
        Long conversationId,
        Long id,
        String text,
        String time,
        Long senderUserId,
        String senderName,
        String type,
        String mediaUrl
) {
}
