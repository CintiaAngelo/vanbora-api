package com.vanbora.api.modules.finance.dto;

import com.vanbora.api.modules.finance.domain.FuelEntry;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Abastecimento exibido na lista. */
public record FuelEntryResponse(
        Long id,
        LocalDate date,
        double liters,
        BigDecimal amount
) {
    public static FuelEntryResponse from(FuelEntry entry) {
        return new FuelEntryResponse(entry.getId(), entry.getDate(), entry.getLiters(), entry.getAmount());
    }
}
