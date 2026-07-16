package com.vanbora.api.shared.fee;

import java.math.BigDecimal;

/**
 * Detalhamento do valor de um plano do ponto de vista do transportador: quanto ele
 * cobra (bruto), quanto o VanBora retém, quanto o gateway retém e quanto sobra líquido.
 */
public record FeeBreakdown(
        BigDecimal gross,
        BigDecimal adminFee,
        BigDecimal gatewayFee,
        BigDecimal net
) {
}
