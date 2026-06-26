package com.vanbora.api.modules.enrollment.repository;

import com.vanbora.api.modules.enrollment.domain.Absence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    Optional<Absence> findByEnrollmentIdAndDate(Long enrollmentId, LocalDate date);

    boolean existsByEnrollmentIdAndDate(Long enrollmentId, LocalDate date);

    List<Absence> findByEnrollmentIdAndDateBetween(Long enrollmentId, LocalDate from, LocalDate to);
}
