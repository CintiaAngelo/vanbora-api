package com.vanbora.api.modules.school.dto;

import jakarta.validation.constraints.NotBlank;

/** Cadastro de uma escola no catálogo (pelo transportador, via CEP). */
public record CreateSchoolRequest(
        @NotBlank String name,
        String cep,
        String street,
        String number,
        String neighborhood,
        String city,
        String uf
) {
}
