package com.vanbora.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Lançamento manual de receita de um mês (yyyy-MM). */
public record SetRevenueRequest(
        @NotNull @Pattern(regexp = "\\d{4}-\\d{2}", message = "Use o formato yyyy-MM")
        String referenceMonth,
        @NotNull @DecimalMin("0.0") BigDecimal amount
) {
}
