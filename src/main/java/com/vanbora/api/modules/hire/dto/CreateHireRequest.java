package com.vanbora.api.modules.hire.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Pedido de contratação enviado pelo responsável. */
public record CreateHireRequest(
        @NotNull Long transporterId,
        @NotNull Long dependentId,
        /** Proposta de valor (opcional; só aceita se o transportador permitir). */
        @DecimalMin("0.0") BigDecimal proposedFee
) {
}
