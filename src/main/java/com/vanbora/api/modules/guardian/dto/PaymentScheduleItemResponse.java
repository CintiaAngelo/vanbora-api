package com.vanbora.api.modules.guardian.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item do cronograma de pagamentos do responsável: mensalidades reais
 * (histórico) e meses futuros projetados.
 */
public record PaymentScheduleItemResponse(
        Long id,
        String referenceMonth,
        BigDecimal amount,
        /** PAID | PENDING | OVERDUE | SCHEDULED */
        String status,
        LocalDate dueDate,
        /** true = mês futuro projetado (ainda não cobrado). */
        boolean projected
) {
}
