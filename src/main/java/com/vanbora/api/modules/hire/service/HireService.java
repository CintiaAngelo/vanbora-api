package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import java.util.List;

/** Casos de uso de solicitações de contratação. */
public interface HireService {

    HireRequestResponse requestHire(Long guardianUserId, CreateHireRequest request);

    List<HireRequestResponse> listPendingForTransporter(Long transporterUserId);

    void accept(Long transporterUserId, Long hireRequestId);

    void reject(Long transporterUserId, Long hireRequestId);

    /** [Responsável] Cancela a própria solicitação pendente (some da tela do transportador). */
    void cancelHireRequest(Long guardianUserId, Long hireRequestId);

    /** [Responsável] Dispensa o aviso de uma solicitação recusada na home. */
    void dismissHireRequest(Long guardianUserId, Long hireRequestId);

    /** Expira as solicitações pendentes cujo prazo de resposta já passou. */
    int expireOverdue();
}
