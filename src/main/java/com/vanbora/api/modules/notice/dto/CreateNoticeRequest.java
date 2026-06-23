package com.vanbora.api.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Criação de um aviso em massa. */
public record CreateNoticeRequest(
        @NotBlank @Size(max = 600) String message
) {
}
