package com.vanbora.api.modules.enrollment.repository;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByTransporterId(Long transporterId);

    List<Enrollment> findByDependentGuardianId(Long guardianId);

    Optional<Enrollment> findFirstByDependentGuardianIdAndActiveTrue(Long guardianId);

    long countByTransporterId(Long transporterId);
}
