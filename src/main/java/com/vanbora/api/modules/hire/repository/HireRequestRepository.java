package com.vanbora.api.modules.hire.repository;

import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.shared.enums.HireStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HireRequestRepository extends JpaRepository<HireRequest, Long> {

    List<HireRequest> findByTransporterIdAndStatus(Long transporterId, HireStatus status);

    long countByTransporterIdAndStatus(Long transporterId, HireStatus status);

    List<HireRequest> findByStatus(HireStatus status);

    /** Solicitação mais recente do dependente (para acompanhar o estado atual na home). */
    Optional<HireRequest> findFirstByGuardianIdAndDependentIdOrderByIdDesc(Long guardianId, Long dependentId);

    /** Solicitação mais recente do dependente em um status específico. */
    Optional<HireRequest> findFirstByGuardianIdAndDependentIdAndStatusOrderByIdDesc(
            Long guardianId, Long dependentId, HireStatus status);
}
