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
        List<MonthlyRevenue> monthlyRevenue,
        PeriodComparison comparison,
        /** Soma de monthlyFee das matrículas ativas — quanto entra por mês se nada mudar. */
        BigDecimal recurringMonthlyRevenue,
        /** Receita recebida x despesas dos últimos 6 meses corridos (gráfico da Visão Geral). */
        List<MonthlyTrend> monthlyTrend
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

    /** Receita recebida x despesas (gastos + combustível) em um dos últimos 6 meses. */
    public record MonthlyTrend(String label, BigDecimal received, BigDecimal expenses) {}

    /**
     * Comparação com o período imediatamente anterior (mesmo tamanho). *ChangePct é null
     * quando o valor anterior é zero (evita divisão por zero / "+infinito%").
     */
    public record PeriodComparison(
            BigDecimal previousReceived, Double receivedChangePct,
            BigDecimal previousExpenses, Double expensesChangePct,
            double previousKm, Double kmChangePct
    ) {}
}
