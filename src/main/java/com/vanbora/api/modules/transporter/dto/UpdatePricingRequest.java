package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/**
 * Configuração de preço do transportador: valores dos 3 planos (mensal/anual/parcelado),
 * se aceita propostas e as zonas de preço por escola/bairro. Campos de plano nulos
 * mantêm o valor atual; {@code annualFee}/{@code installmentMonthlyFee} nulos = plano não oferecido.
 */
public record UpdatePricingRequest(
        @DecimalMin("0.0") BigDecimal monthlyFee,
        @DecimalMin("0.0") BigDecimal annualFee,
        @DecimalMin("0.0") BigDecimal installmentMonthlyFee,
        boolean acceptsProposals,
        List<PriceZoneInput> zones
) {
}
