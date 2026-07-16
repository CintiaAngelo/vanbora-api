package com.vanbora.api.shared.fee;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Taxas do VanBora (lidas de application.yml / variáveis de ambiente). Fonte única
 * das porcentagens usadas no cálculo de mensalidade líquida, multa e reembolso —
 * também expostas ao app por {@code GET /api/config/fees}.
 *
 * @param adminMonthly    taxa administrativa VanBora sobre a mensalidade (0.05 = 5%)
 * @param adminAnnual     taxa administrativa VanBora sobre o plano anual à vista (0.04 = 4%)
 * @param gatewayRate     taxa percentual do gateway de pagamento por transação (0.0498 ≈ 4,98%)
 * @param gatewayFixed    taxa fixa do gateway por transação (R$)
 * @param fidelityMonths  meses de fidelidade do plano parcelado (e base do anual)
 * @param installmentFine fração da soma das mensalidades restantes cobrada como multa (0.50 = 50%)
 */
@ConfigurationProperties(prefix = "vanbora.fees")
public record FeeProperties(
        @DefaultValue("0.05") BigDecimal adminMonthly,
        @DefaultValue("0.04") BigDecimal adminAnnual,
        @DefaultValue("0.0498") BigDecimal gatewayRate,
        @DefaultValue("0.00") BigDecimal gatewayFixed,
        @DefaultValue("12") int fidelityMonths,
        @DefaultValue("0.50") BigDecimal installmentFine
) {
}
