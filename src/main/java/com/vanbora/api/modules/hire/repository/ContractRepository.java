package com.vanbora.api.modules.hire.repository;

import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.shared.enums.ContractStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByGuardianIdOrderByIdDesc(Long guardianId);

    List<Contract> findByGuardianIdAndStatusOrderByIdDesc(Long guardianId, ContractStatus status);

    Optional<Contract> findByIdAndGuardianId(Long id, Long guardianId);
}
