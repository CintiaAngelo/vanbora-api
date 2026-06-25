package com.vanbora.api.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Novo comentário em um aviso. */
public record AddCommentRequest(
        @NotBlank @Size(max = 600) String text
) {
}
