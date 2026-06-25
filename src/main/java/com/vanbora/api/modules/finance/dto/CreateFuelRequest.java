package com.vanbora.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de abastecimento (litros + valor). */
public record CreateFuelRequest(
        LocalDate date,
        @Positive double liters,
        @NotNull @DecimalMin("0.0") BigDecimal amount
) {
}
