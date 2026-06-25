package com.vanbora.api.modules.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Relatório detalhado de um período, base para a exportação em PDF (gerada no app). */
public record FinanceReportResponse(
        LocalDate from,
        LocalDate to,
        String transporterName,
        BigDecimal totalReceived,
        BigDecimal totalExpenses,
        BigDecimal balance,
        double km,
        List<ReportLine> income,
        List<ReportLine> expenses
) {
    /** Linha do relatório (data, descrição, valor). */
    public record ReportLine(LocalDate date, String description, BigDecimal amount) {}
}
