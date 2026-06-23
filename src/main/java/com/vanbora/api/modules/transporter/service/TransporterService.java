package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import java.util.List;

/** Casos de uso relacionados ao transportador (busca pública e perfil próprio). */
public interface TransporterService {

    List<TransporterSummaryResponse> search(String school, String neighborhood, String sort);

    TransporterDetailResponse getPublicProfile(Long transporterId);

    TransporterProfileResponse getMyProfile(Long userId);
}
