package com.vanbora.api.modules.finance.repository;

import com.vanbora.api.modules.finance.domain.ManualRevenue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualRevenueRepository extends JpaRepository<ManualRevenue, Long> {

    List<ManualRevenue> findByTransporterId(Long transporterId);

    Optional<ManualRevenue> findByTransporterIdAndReferenceMonth(Long transporterId, String referenceMonth);
}
