package com.vanbora.api.modules.auth.dto;

import com.vanbora.api.modules.guardian.dto.AddressInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de cadastro do responsável, com endereço de embarque e de entrega. */
public record RegisterGuardianRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres") String password,
        @NotBlank String phone,
        String cpf,
        /** Endereço de embarque (buscar a criança). */
        AddressInput pickup,
        /** Se a entrega é no mesmo endereço do embarque. */
        boolean deliverySameAsPickup,
        /** Endereço de entrega (quando diferente do embarque). */
        AddressInput delivery
) {
}
