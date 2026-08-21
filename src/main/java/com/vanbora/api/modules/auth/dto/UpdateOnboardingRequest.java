package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Marca que o usuário viu (ou dispensou) uma versão do guia de funcionalidades. */
public record UpdateOnboardingRequest(
        @NotNull @Min(0) Integer seenVersion
) {
}
