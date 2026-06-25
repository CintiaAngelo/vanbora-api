package com.vanbora.api.modules.finance.dto;

import com.vanbora.api.modules.finance.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto exibido na lista. */
public record ExpenseResponse(
        Long id,
        LocalDate date,
        String category,
        String description,
        BigDecimal amount
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(), expense.getDate(), expense.getCategory(),
                expense.getDescription(), expense.getAmount());
    }
}
