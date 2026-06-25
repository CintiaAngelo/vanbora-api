package com.vanbora.api.modules.finance.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/** Ajustes financeiros: meta de receita mensal e intervalo de manutenção (km). */
public record FinanceSettingsRequest(
        BigDecimal monthlyRevenueGoal,
        @Min(0) Integer maintenanceIntervalKm
) {
}
