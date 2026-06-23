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
}
