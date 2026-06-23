package com.vanbora.api.modules.finance.service;

import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;

/** Caso de uso do painel financeiro do transportador. */
public interface FinanceService {

    FinanceSummaryResponse getSummary(Long transporterUserId);
}
