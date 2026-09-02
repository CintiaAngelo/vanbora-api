package com.vanbora.api.modules.auth.dto;

import com.vanbora.api.modules.guardian.dto.AddressInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Dados de cadastro do responsável, com endereço de embarque e de entrega. */
public record RegisterGuardianRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        /**
         * Senha. Obrigatória (mínimo 6 caracteres) no cadastro comum; ignorada
         * quando vem um {@code socialTicket} — a validação é feita no serviço,
         * porque depende do outro campo.
         */
        String password,
        /** Ticket do login social (Google/Facebook), quando o cadastro veio de lá. */
        String socialTicket,
        @NotBlank String phone,
        String cpf,
        /** Endereço de embarque (buscar a criança). */
        AddressInput pickup,
        /** Se a entrega é no mesmo endereço do embarque. */
        boolean deliverySameAsPickup,
        /** Endereço de entrega (quando diferente do embarque). */
        AddressInput delivery,
        /** Aceite da Política de Privacidade + Termos de Uso (LGPD). Obrigatório. */
        boolean acceptedTerms
) {
}
