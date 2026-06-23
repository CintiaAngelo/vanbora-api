package com.vanbora.api.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumo financeiro do transportador. */
public record FinanceSummaryResponse(
        BigDecimal receivedThisMonth,
        BigDecimal pending,
        BigDecimal overdue,
        List<MonthlyRevenue> monthlyRevenue
) {
    /** Receita recebida em um mês (rótulo curto + valor). */
    public record MonthlyRevenue(String label, BigDecimal value) {
    }
}
