package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Dados de cadastro do transportador (dados pessoais/veículo + área e preço). */
public record RegisterTransporterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres") String password,
        @NotBlank String phone,
        String document,
        String cnh,
        String plate,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal baseMonthlyFee,
        /** Aceite da Política de Privacidade + Termos de Uso (LGPD). Obrigatório. */
        boolean acceptedTerms
) {
}
