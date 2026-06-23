package com.vanbora.api.modules.hire.repository;

import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.shared.enums.HireStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HireRequestRepository extends JpaRepository<HireRequest, Long> {

    List<HireRequest> findByTransporterIdAndStatus(Long transporterId, HireStatus status);

    long countByTransporterIdAndStatus(Long transporterId, HireStatus status);
}
