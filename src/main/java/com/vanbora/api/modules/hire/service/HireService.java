package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.hire.dto.CounterProposalRequest;
import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.GuardianForTransporterResponse;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import java.util.List;

/** Casos de uso de solicitações de contratação. */
public interface HireService {

    HireRequestResponse requestHire(Long guardianUserId, CreateHireRequest request);

    List<HireRequestResponse> listPendingForTransporter(Long transporterUserId);

    /** [Transportador] Perfil do responsável de uma solicitação que lhe pertence. */
    GuardianForTransporterResponse getGuardianForRequest(Long transporterUserId, Long hireRequestId);

    void accept(Long transporterUserId, Long hireRequestId);

    void reject(Long transporterUserId, Long hireRequestId);

    /** [Transportador] Contrapropõe outro valor em resposta à proposta do responsável. */
    void counterPropose(Long transporterUserId, Long hireRequestId, CounterProposalRequest request);

    /** [Responsável] Aceita a contraproposta do transportador — libera o contrato como um accept normal. */
    void acceptCounterProposal(Long guardianUserId, Long hireRequestId);

    /** [Responsável] Recusa a contraproposta do transportador — encerra a negociação. */
    void rejectCounterProposal(Long guardianUserId, Long hireRequestId);

    /** [Responsável] Cancela a própria solicitação pendente (some da tela do transportador). */
    void cancelHireRequest(Long guardianUserId, Long hireRequestId);

    /** [Responsável] Dispensa o aviso de uma solicitação recusada na home. */
    void dismissHireRequest(Long guardianUserId, Long hireRequestId);

    /** Expira as solicitações pendentes cujo prazo de resposta já passou. */
    int expireOverdue();
}
