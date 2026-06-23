package com.vanbora.api.modules.finance;

import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;
import com.vanbora.api.modules.finance.service.FinanceService;
import com.vanbora.api.security.CurrentUserProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public FinanceSummaryResponse summary() {
        return financeService.getSummary(currentUserProvider.requireUserId());
    }
}
