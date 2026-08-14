package com.vanbora.api.modules.finance.service;

import com.vanbora.api.modules.finance.dto.CreateExpenseRequest;
import com.vanbora.api.modules.finance.dto.CreateFuelRequest;
import com.vanbora.api.modules.finance.dto.ExpenseResponse;
import com.vanbora.api.modules.finance.dto.FinanceBreakdownItem;
import com.vanbora.api.modules.finance.dto.FinanceReportResponse;
import com.vanbora.api.modules.finance.dto.FinanceSettingsRequest;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;
import com.vanbora.api.modules.finance.dto.FuelEntryResponse;
import com.vanbora.api.modules.finance.dto.PaymentContactResponse;
import com.vanbora.api.modules.finance.dto.SetRevenueRequest;
import com.vanbora.api.modules.finance.dto.SuggestionResponse;
import java.time.LocalDate;
import java.util.List;

/** Casos de uso do painel financeiro do transportador. */
public interface FinanceService {

    FinanceSummaryResponse getSummary(Long transporterUserId, LocalDate from, LocalDate to);

    FinanceReportResponse getReport(Long transporterUserId, LocalDate from, LocalDate to);

    List<SuggestionResponse> getSuggestions(Long transporterUserId);

    /** Detalhamento (pendente/vencido/recebido) por aluno, para os tiles clicáveis. */
    List<FinanceBreakdownItem> getBreakdown(Long transporterUserId, String type);

    /** Lança/atualiza a receita manual de um mês (backfill). */
    void setManualRevenue(Long transporterUserId, SetRevenueRequest request);

    /** Pagamentos vencidos do transportador, com dados do aluno/responsável para cobrança. */
    List<PaymentContactResponse> listOverduePayments(Long transporterUserId);

    /** Pagamentos ainda dentro do prazo do transportador, com dados do aluno/responsável. */
    List<PaymentContactResponse> listPendingPayments(Long transporterUserId);

    // Gastos
    List<ExpenseResponse> listExpenses(Long transporterUserId, LocalDate from, LocalDate to);

    ExpenseResponse addExpense(Long transporterUserId, CreateExpenseRequest request);

    ExpenseResponse updateExpense(Long transporterUserId, Long expenseId, CreateExpenseRequest request);

    void deleteExpense(Long transporterUserId, Long expenseId);

    // Combustível
    List<FuelEntryResponse> listFuel(Long transporterUserId, LocalDate from, LocalDate to);

    FuelEntryResponse addFuel(Long transporterUserId, CreateFuelRequest request);

    void deleteFuel(Long transporterUserId, Long fuelId);

    // Ajustes / manutenção
    void updateSettings(Long transporterUserId, FinanceSettingsRequest request);

    void markMaintenanceDone(Long transporterUserId);
}
