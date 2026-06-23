package com.vanbora.api.modules.guardian.service;

import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import java.util.List;

/** Casos de uso do responsável. */
public interface GuardianService {

    GuardianProfileResponse getMyProfile(Long userId);

    List<DependentResponse> listDependents(Long userId);

    DependentResponse addDependent(Long userId, CreateDependentRequest request);

    GuardianDashboardResponse getDashboard(Long userId);

    List<PaymentResponse> listPayments(Long userId);
}
