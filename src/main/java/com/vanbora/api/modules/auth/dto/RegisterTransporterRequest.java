package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de cadastro do transportador (etapa 1). */
public record RegisterTransporterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres") String password,
        @NotBlank String phone,
        String document,
        String cnh,
        String plate,
        Integer capacity
) {
}
