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

    /** Há (ou houve) contrato deste responsável com este transportador? (elegibilidade de avaliação) */
    boolean existsByGuardianIdAndTransporterId(Long guardianId, Long transporterId);

    /** Contratos deste responsável com este transportador num dado status (para cancelar todos). */
    List<Contract> findByGuardianIdAndTransporterIdAndStatus(
            Long guardianId, Long transporterId, ContractStatus status);

    /** Contrato mais recente de um dependente num status (ex.: pendente de assinatura). */
    Optional<Contract> findFirstByGuardianIdAndDependentIdAndStatusOrderByIdDesc(
            Long guardianId, Long dependentId, ContractStatus status);
}
