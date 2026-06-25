package com.vanbora.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Inserção/edição de um gasto livre. */
public record CreateExpenseRequest(
        LocalDate date,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 200) String description,
        @NotNull @DecimalMin("0.0") BigDecimal amount
) {
}
