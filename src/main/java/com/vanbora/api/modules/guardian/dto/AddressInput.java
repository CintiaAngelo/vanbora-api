package com.vanbora.api.modules.guardian.dto;

/** Endereço informado pelo responsável (já com dados do CEP preenchidos no app). */
public record AddressInput(
        String cep,
        String street,
        String number,
        String neighborhood,
        String city
) {
}
