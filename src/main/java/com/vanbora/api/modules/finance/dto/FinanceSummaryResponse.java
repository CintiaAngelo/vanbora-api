package com.vanbora.api.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumo financeiro ampliado do transportador (período + km + despesas + metas). */
public record FinanceSummaryResponse(
        BigDecimal received,
        BigDecimal pending,
        BigDecimal overdue,
        BigDecimal expenses,
        BigDecimal balance,
        double kmToday,
        double kmPeriod,
        double kmTotal,
        List<CategoryTotal> expensesByCategory,
        Consumption consumption,
        Goal goal,
        Maintenance maintenance,
        List<MonthlyRevenue> monthlyRevenue
) {
    /** Total gasto em uma categoria. */
    public record CategoryTotal(String category, BigDecimal total) {}

    /** Consumo de combustível no período. */
    public record Consumption(double liters, Double kmPerLiter, BigDecimal costPerKm) {}

    /** Meta de receita mensal e progresso (0..1). */
    public record Goal(BigDecimal monthlyGoal, BigDecimal monthRevenue, double progress) {}

    /** Manutenção por km (intervalo, km rodados desde a última, quanto falta). */
    public record Maintenance(Integer intervalKm, Double kmSinceLast, Double remainingKm) {}

    /** Receita recebida em um mês (rótulo curto + valor). */
    public record MonthlyRevenue(String label, BigDecimal value) {}
}
