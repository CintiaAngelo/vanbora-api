package com.vanbora.api.modules.chat.dto;

/** Mensagem exibida na conversa. */
public record MessageResponse(
        Long id,
        String text,
        String time,
        boolean fromMe
) {
}
