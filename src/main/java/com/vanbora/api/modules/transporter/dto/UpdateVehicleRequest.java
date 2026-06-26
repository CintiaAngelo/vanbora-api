package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.Size;

/** Edição dos dados do veículo do transportador. */
public record UpdateVehicleRequest(
        @Size(max = 30) String cnh,
        @Size(max = 15) String plate
) {
}
