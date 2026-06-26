package com.vanbora.api.modules.enrollment.repository;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByTransporterId(Long transporterId);

    List<Enrollment> findByDependentGuardianId(Long guardianId);

    Optional<Enrollment> findFirstByDependentGuardianIdAndActiveTrue(Long guardianId);

    /** Matrícula ativa de um dependente específico do responsável (visão por dependente). */
    Optional<Enrollment> findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(
            Long dependentId, Long guardianId);

    /** Existe (ou existiu) vínculo deste responsável com este transportador? (elegibilidade de avaliação) */
    boolean existsByDependentGuardianIdAndTransporterId(Long guardianId, Long transporterId);

    /** Matrículas ativas deste responsável com este transportador (para o cancelamento total). */
    List<Enrollment> findByDependentGuardianIdAndTransporterIdAndActiveTrue(
            Long guardianId, Long transporterId);

    long countByTransporterId(Long transporterId);
}
