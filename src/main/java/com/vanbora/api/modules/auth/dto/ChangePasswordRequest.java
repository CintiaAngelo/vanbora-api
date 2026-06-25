package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Troca de senha do usuário autenticado. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6, message = "A nova senha deve ter ao menos 6 caracteres.") String newPassword
) {
}
