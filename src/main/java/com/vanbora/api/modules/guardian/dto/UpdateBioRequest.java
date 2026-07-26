package com.vanbora.api.modules.guardian.dto;

import jakarta.validation.constraints.Size;

/** Atualização da descrição/biografia do responsável. */
public record UpdateBioRequest(
        @Size(max = 1000) String bio
) {
}
