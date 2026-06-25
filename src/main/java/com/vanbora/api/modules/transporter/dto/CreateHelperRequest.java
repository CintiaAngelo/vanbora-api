package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.NotBlank;

/** Cadastro de um ajudante/monitor do transportador. */
public record CreateHelperRequest(
        @NotBlank String name,
        @NotBlank String role
) {
}
