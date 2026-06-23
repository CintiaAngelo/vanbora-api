package com.vanbora.api.modules.payment.dto;

import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.shared.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Mensalidade exibida no histórico/painel de pagamentos. */
public record PaymentResponse(
        Long id,
        String referenceMonth,
        BigDecimal amount,
        PaymentStatus status,
        LocalDate dueDate
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReferenceMonth(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getDueDate());
    }
}
