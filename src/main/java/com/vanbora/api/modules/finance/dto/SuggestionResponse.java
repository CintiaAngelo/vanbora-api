package com.vanbora.api.modules.finance.dto;

import java.math.BigDecimal;

/** Sugestão de gasto/ação gerada por regra. */
public record SuggestionResponse(
        String title,
        String detail,
        String suggestedCategory,
        BigDecimal suggestedAmount
) {
}
