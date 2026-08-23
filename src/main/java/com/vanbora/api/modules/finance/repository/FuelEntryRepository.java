package com.vanbora.api.modules.finance.repository;

import com.vanbora.api.modules.finance.domain.FuelEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {

    List<FuelEntry> findByTransporterIdOrderByDateDesc(Long transporterId);

    List<FuelEntry> findByTransporterIdAndDateBetweenOrderByDateDesc(Long transporterId,
                                                                     LocalDate from, LocalDate to);

    /** Escopado pelo transportador dono — evita depender de uma checagem manual de posse. */
    Optional<FuelEntry> findByIdAndTransporterId(Long id, Long transporterId);
}
