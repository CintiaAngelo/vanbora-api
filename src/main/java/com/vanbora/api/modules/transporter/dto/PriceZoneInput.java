package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/**
 * Zona de preço enviada pelo transportador ao salvar a precificação.
 * {@code id} nulo cria uma zona nova; ids ausentes na lista são removidos.
 */
public record PriceZoneInput(
        Long id,
        String name,
        String school,
        List<String> neighborhoods,
        @DecimalMin("0.0") BigDecimal monthlyFee,
        @DecimalMin("0.0") BigDecimal annualFee,
        @DecimalMin("0.0") BigDecimal installmentMonthlyFee
) {
}
