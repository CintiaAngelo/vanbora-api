package com.vanbora.api.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reação (emoji) do responsável a um aviso. */
public record ReactionRequest(
        @NotBlank @Size(max = 16) String emoji
) {
}
