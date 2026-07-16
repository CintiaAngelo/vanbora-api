package com.vanbora.api.modules.config;

import com.vanbora.api.shared.fee.FeeProperties;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configurações públicas do app. Expõe as taxas do VanBora para que o app calcule
 * as prévias (mensalidade líquida, multa, reembolso) sem duplicar as porcentagens.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final FeeProperties fees;

    public ConfigController(FeeProperties fees) {
        this.fees = fees;
    }

    @GetMapping("/fees")
    public FeesResponse fees() {
        return new FeesResponse(
                fees.adminMonthly(),
                fees.adminAnnual(),
                fees.gatewayRate(),
                fees.gatewayFixed(),
                fees.fidelityMonths(),
                fees.installmentFine());
    }

    /** Taxas expostas ao app (frações; ex.: 0.05 = 5%). */
    public record FeesResponse(
            BigDecimal adminMonthly,
            BigDecimal adminAnnual,
            BigDecimal gatewayRate,
            BigDecimal gatewayFixed,
            int fidelityMonths,
            BigDecimal installmentFine
    ) {
    }
}
