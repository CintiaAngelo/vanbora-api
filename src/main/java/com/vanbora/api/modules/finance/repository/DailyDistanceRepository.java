package com.vanbora.api.modules.finance.repository;

import com.vanbora.api.modules.finance.domain.DailyDistance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyDistanceRepository extends JpaRepository<DailyDistance, Long> {

    Optional<DailyDistance> findByTransporterIdAndDate(Long transporterId, LocalDate date);

    List<DailyDistance> findByTransporterIdAndDateBetween(Long transporterId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(d.km), 0) from DailyDistance d where d.transporter.id = :transporterId")
    double totalKm(@Param("transporterId") Long transporterId);
}
