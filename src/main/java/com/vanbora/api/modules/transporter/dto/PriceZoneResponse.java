package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.PriceZone;
import java.math.BigDecimal;
import java.util.List;

/** Zona de preço do transportador (visão de edição). */
public record PriceZoneResponse(
        Long id,
        String name,
        String school,
        List<String> neighborhoods,
        BigDecimal monthlyFee,
        BigDecimal annualFee,
        BigDecimal installmentMonthlyFee
) {
    public static PriceZoneResponse from(PriceZone zone) {
        return new PriceZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getSchool(),
                List.copyOf(zone.getNeighborhoods()),
                zone.getMonthlyFee(),
                zone.getAnnualFee(),
                zone.getInstallmentMonthlyFee());
    }
}
