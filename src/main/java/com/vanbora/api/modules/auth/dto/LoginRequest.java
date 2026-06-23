package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenciais de acesso. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
