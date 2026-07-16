package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.shared.enums.PlanType;
import java.math.BigDecimal;

/**
 * Prévia da rescisão exibida ao responsável antes de confirmar: mostra a multa (se
 * houver fidelidade ativa) ou o reembolso (plano anual), e se exige pagamento.
 */
public record CancellationPreviewResponse(
        PlanType planType,
        int monthsElapsed,
        boolean fidelityActive,
        BigDecimal fineAmount,
        BigDecimal refundAmount,
        boolean requiresPayment,
        String message
) {
}
