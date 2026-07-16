package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Janela de compartilhamento enviada pelo transportador (horas em "HH:mm"). */
public record LocationWindowInput(
        @Min(1) @Max(7) int dayOfWeek,
        @NotBlank String startTime,
        @NotBlank String endTime
) {
}
