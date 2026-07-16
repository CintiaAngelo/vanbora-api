package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.shared.enums.PlanType;
import jakarta.validation.constraints.NotNull;

/**
 * Assinatura do contrato pelo responsável: meio de pagamento salvo + plano escolhido.
 * {@code planType} nulo assume MONTHLY (compatibilidade).
 */
public record SignContractRequest(
        @NotNull Long paymentMethodId,
        PlanType planType
) {
}
