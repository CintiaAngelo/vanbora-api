package com.vanbora.api.shared.fee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Centraliza todo o cálculo financeiro do VanBora: mensalidade líquida ao
 * transportador (descontadas as taxas administrativa e do gateway), multa de
 * rescisão do parcelado e reembolso proporcional do anual. As porcentagens vêm de
 * {@link FeeProperties} — nunca são números mágicos espalhados pelo código.
 */
@Component
public class FeeCalculator {

    private static final int MONEY_SCALE = 2;

    private final FeeProperties fees;

    public FeeCalculator(FeeProperties fees) {
        this.fees = fees;
    }

    public FeeProperties properties() {
        return fees;
    }

    /** Detalha um valor mensal (planos mensal e parcelado). */
    public FeeBreakdown monthlyBreakdown(BigDecimal gross) {
        return breakdown(gross, fees.adminMonthly());
    }

    /** Detalha o valor total do plano anual pago à vista. */
    public FeeBreakdown annualBreakdown(BigDecimal total) {
        return breakdown(total, fees.adminAnnual());
    }

    private FeeBreakdown breakdown(BigDecimal gross, BigDecimal adminRate) {
        BigDecimal value = gross == null ? BigDecimal.ZERO : gross;
        BigDecimal admin = money(value.multiply(adminRate));
        BigDecimal gateway = money(value.multiply(fees.gatewayRate()).add(fees.gatewayFixed()));
        BigDecimal net = money(value.subtract(admin).subtract(gateway));
        return new FeeBreakdown(money(value), admin, gateway, net);
    }

    /**
     * Multa de rescisão do parcelado: {@code installmentFine} (50%) da soma das
     * mensalidades que faltam até completar a fidelidade.
     */
    public BigDecimal installmentFine(BigDecimal installmentMonthly, int monthsElapsed) {
        if (installmentMonthly == null) {
            return BigDecimal.ZERO;
        }
        int remaining = Math.max(0, fees.fidelityMonths() - Math.max(0, monthsElapsed));
        return money(installmentMonthly
                .multiply(BigDecimal.valueOf(remaining))
                .multiply(fees.installmentFine()));
    }

    /**
     * Reembolso proporcional do plano anual: devolve a fração dos meses não usados,
     * já descontada a taxa administrativa que o VanBora retém sobre o total.
     */
    public BigDecimal annualRefund(BigDecimal total, int monthsUsed) {
        if (total == null) {
            return BigDecimal.ZERO;
        }
        int months = fees.fidelityMonths();
        int used = Math.min(Math.max(0, monthsUsed), months);
        int unused = months - used;
        if (unused <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal netOfAdmin = total.subtract(total.multiply(fees.adminAnnual()));
        BigDecimal fraction = BigDecimal.valueOf(unused)
                .divide(BigDecimal.valueOf(months), 6, RoundingMode.HALF_UP);
        return money(netOfAdmin.multiply(fraction));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
