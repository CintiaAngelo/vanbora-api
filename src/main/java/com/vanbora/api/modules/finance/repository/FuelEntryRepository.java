package com.vanbora.api.modules.finance.repository;

import com.vanbora.api.modules.finance.domain.FuelEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {

    List<FuelEntry> findByTransporterIdOrderByDateDesc(Long transporterId);

    List<FuelEntry> findByTransporterIdAndDateBetweenOrderByDateDesc(Long transporterId,
                                                                     LocalDate from, LocalDate to);
}
