package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.shared.enums.ContractStatus;
import java.util.List;

/** Casos de uso de contrato do responsável. */
public interface ContractService {

    List<ContractResponse> listForGuardian(Long userId, ContractStatus status);

    ContractResponse get(Long userId, Long contractId);

    /**
     * Assina o contrato com o meio de pagamento escolhido: cobra a 1ª mensalidade
     * via gateway, cria a matrícula ativa + o primeiro pagamento e marca o
     * contrato como ACTIVE.
     */
    ContractResponse sign(Long userId, Long contractId, Long paymentMethodId);
}
