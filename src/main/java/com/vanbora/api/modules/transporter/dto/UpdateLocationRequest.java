package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Posição GPS enviada pelo app do transportador. */
public record UpdateLocationRequest(
        @NotNull
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double longitude,

        /** Direção do deslocamento em graus (0-360), quando o GPS do aparelho fornece. */
        @DecimalMin(value = "0.0") @DecimalMax(value = "360.0")
        Double heading
) {
}
