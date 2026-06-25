package com.vanbora.api.modules.finance.repository;

import com.vanbora.api.modules.finance.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByTransporterIdOrderByDateDesc(Long transporterId);

    List<Expense> findByTransporterIdAndDateBetweenOrderByDateDesc(Long transporterId,
                                                                   LocalDate from, LocalDate to);
}
