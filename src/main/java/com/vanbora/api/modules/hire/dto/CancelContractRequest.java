package com.vanbora.api.modules.hire.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Cancelamento (rescisão) de contrato: a avaliação do transportador é obrigatória.
 * {@code paymentMethodId} é exigido apenas quando há multa de fidelidade a pagar.
 */
public record CancelContractRequest(
        @Min(value = 1, message = "A nota deve ser de 1 a 5 estrelas")
        @Max(value = 5, message = "A nota deve ser de 1 a 5 estrelas")
        int rating,
        @Size(max = 500) String comment,
        Long paymentMethodId
) {
}
