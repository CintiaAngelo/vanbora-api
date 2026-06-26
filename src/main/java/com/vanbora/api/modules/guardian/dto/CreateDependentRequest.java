package com.vanbora.api.modules.guardian.dto;

import jakarta.validation.constraints.NotBlank;

/** Cadastro/edição de um dependente. `schoolId` referencia o catálogo (opcional). */
public record CreateDependentRequest(
        @NotBlank String name,
        @NotBlank String school,
        Long schoolId
) {
}
