package com.vanbora.api.modules.finance;

import com.vanbora.api.modules.finance.dto.CreateExpenseRequest;
import com.vanbora.api.modules.finance.dto.CreateFuelRequest;
import com.vanbora.api.modules.finance.dto.ExpenseResponse;
import com.vanbora.api.modules.finance.dto.FinanceBreakdownItem;
import com.vanbora.api.modules.finance.dto.FinanceReportResponse;
import com.vanbora.api.modules.finance.dto.FinanceSettingsRequest;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;
import com.vanbora.api.modules.finance.dto.FuelEntryResponse;
import com.vanbora.api.modules.finance.dto.SetRevenueRequest;
import com.vanbora.api.modules.finance.dto.SuggestionResponse;
import com.vanbora.api.modules.finance.service.FinanceService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Painel financeiro do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me/finance")
@PreAuthorize("hasRole('TRANSPORTER')")
public class FinanceController {

    private final FinanceService financeService;
    private final CurrentUserProvider currentUserProvider;

    public FinanceController(FinanceService financeService, CurrentUserProvider currentUserProvider) {
        this.financeService = financeService;
        this.currentUserProvider = currentUserProvider;
    }

    private Long userId() {
        return currentUserProvider.requireUserId();
    }

    @GetMapping
    public FinanceSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.getSummary(userId(), from, to);
    }

    @GetMapping("/report")
    public FinanceReportResponse report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.getReport(userId(), from, to);
    }

    @GetMapping("/suggestions")
    public List<SuggestionResponse> suggestions() {
        return financeService.getSuggestions(userId());
    }

    /** Detalhamento por aluno: type = pending | overdue | received. */
    @GetMapping("/breakdown")
    public List<FinanceBreakdownItem> breakdown(@RequestParam(defaultValue = "received") String type) {
        return financeService.getBreakdown(userId(), type);
    }

    /** Lança/atualiza a receita manual de um mês (backfill de meses anteriores). */
    @PostMapping("/revenue")
    public ResponseEntity<Void> setRevenue(@Valid @RequestBody SetRevenueRequest request) {
        financeService.setManualRevenue(userId(), request);
        return ResponseEntity.noContent().build();
    }

    // ----- Gastos -----

    @GetMapping("/expenses")
    public List<ExpenseResponse> expenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.listExpenses(userId(), from, to);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.addExpense(userId(), request));
    }

    @PutMapping("/expenses/{id}")
    public ExpenseResponse updateExpense(@PathVariable Long id, @Valid @RequestBody CreateExpenseRequest request) {
        return financeService.updateExpense(userId(), id, request);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        financeService.deleteExpense(userId(), id);
        return ResponseEntity.noContent().build();
    }

    // ----- Combustível -----

    @GetMapping("/fuel")
    public List<FuelEntryResponse> fuel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.listFuel(userId(), from, to);
    }

    @PostMapping("/fuel")
    public ResponseEntity<FuelEntryResponse> addFuel(@Valid @RequestBody CreateFuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.addFuel(userId(), request));
    }

    @DeleteMapping("/fuel/{id}")
    public ResponseEntity<Void> deleteFuel(@PathVariable Long id) {
        financeService.deleteFuel(userId(), id);
        return ResponseEntity.noContent().build();
    }

    // ----- Ajustes / manutenção -----

    @PutMapping("/settings")
    public ResponseEntity<Void> settings(@Valid @RequestBody FinanceSettingsRequest request) {
        financeService.updateSettings(userId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/maintenance-done")
    public ResponseEntity<Void> maintenanceDone() {
        financeService.markMaintenanceDone(userId());
        return ResponseEntity.noContent().build();
    }
}
