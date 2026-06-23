package com.vanbora.api.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Envio de uma nova mensagem. */
public record SendMessageRequest(
        @NotBlank @Size(max = 1000) String text
) {
}
