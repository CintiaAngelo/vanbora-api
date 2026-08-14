package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.hire.dto.CancellationPreviewResponse;
import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.PlanType;
import java.util.List;

/** Casos de uso de contrato do responsável. */
public interface ContractService {

    List<ContractResponse> listForGuardian(Long userId, ContractStatus status);

    /** Contratos do transportador autenticado (ex.: histórico de cancelados). */
    List<ContractResponse> listForTransporter(Long userId, ContractStatus status);

    ContractResponse get(Long userId, Long contractId);

    /**
     * Assina o contrato com o plano e o meio de pagamento escolhidos: cobra a 1ª
     * cobrança via gateway (mensalidade ou total anual), cria a matrícula ativa + o
     * primeiro pagamento e marca o contrato como ACTIVE.
     */
    ContractResponse sign(Long userId, Long contractId, Long paymentMethodId, PlanType planType);

    /** Prévia da rescisão: multa (fidelidade) ou reembolso (anual) e se exige pagamento. */
    CancellationPreviewResponse cancellationPreview(Long userId, Long contractId);

    /**
     * Cancela (rescinde) um contrato ativo: registra a avaliação obrigatória, cobra a
     * multa quando há fidelidade ativa, registra o reembolso do anual, encerra a
     * matrícula, notifica o transportador e marca o contrato como CANCELLED.
     */
    ContractResponse cancel(Long userId, Long contractId, int rating, String comment, Long paymentMethodId);
}
