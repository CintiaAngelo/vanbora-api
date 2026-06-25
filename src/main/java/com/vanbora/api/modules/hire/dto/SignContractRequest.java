package com.vanbora.api.modules.hire.dto;

import jakarta.validation.constraints.NotNull;

/** Assinatura do contrato pelo responsável, escolhendo o meio de pagamento salvo. */
public record SignContractRequest(
        @NotNull Long paymentMethodId
) {
}
