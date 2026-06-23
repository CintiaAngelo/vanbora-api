package com.vanbora.api.modules.hire.dto;

import jakarta.validation.constraints.NotNull;

/** Pedido de contratação enviado pelo responsável. */
public record CreateHireRequest(
        @NotNull Long transporterId,
        @NotNull Long dependentId
) {
}
