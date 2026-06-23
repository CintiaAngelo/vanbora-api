package com.vanbora.api.modules.guardian.dto;

import jakarta.validation.constraints.NotBlank;

/** Cadastro de um novo dependente. */
public record CreateDependentRequest(
        @NotBlank String name,
        @NotBlank String school
) {
}
