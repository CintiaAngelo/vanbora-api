package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.NotBlank;

/** Edição de um ajudante (nome, função e situação ativo/inativo). */
public record UpdateHelperRequest(
        @NotBlank String name,
        @NotBlank String role,
        /** null = mantém a situação atual. */
        Boolean active
) {
}
