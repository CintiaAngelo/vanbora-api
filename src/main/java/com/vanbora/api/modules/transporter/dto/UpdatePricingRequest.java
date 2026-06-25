package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** Configuração de preço do transportador: valor de tabela e se aceita propostas. */
public record UpdatePricingRequest(
        @DecimalMin("0.0") BigDecimal baseMonthlyFee,
        boolean acceptsProposals
) {
}
