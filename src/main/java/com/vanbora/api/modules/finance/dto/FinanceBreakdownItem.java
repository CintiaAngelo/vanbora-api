package com.vanbora.api.modules.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Item do detalhamento financeiro (pendente/vencido/recebido) por aluno. */
public record FinanceBreakdownItem(
        String student,
        String referenceMonth,
        BigDecimal amount,
        LocalDate date,
        /** PAID | PENDING | OVERDUE */
        String status
) {
}
