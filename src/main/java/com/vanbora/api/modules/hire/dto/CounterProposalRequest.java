package com.vanbora.api.modules.hire.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Contraproposta de valor enviada pelo transportador em resposta à proposta do responsável. */
public record CounterProposalRequest(
        @NotNull @DecimalMin("0.0") BigDecimal fee
) {
}
