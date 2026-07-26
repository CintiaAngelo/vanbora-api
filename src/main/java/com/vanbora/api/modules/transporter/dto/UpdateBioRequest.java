package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.Size;

/** Atualização da descrição/biografia do transportador. */
public record UpdateBioRequest(
        @Size(max = 1000) String bio
) {
}
