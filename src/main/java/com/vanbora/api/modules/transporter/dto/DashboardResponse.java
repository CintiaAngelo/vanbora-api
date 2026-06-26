package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import java.util.List;

/** Visão gerencial inicial do transportador. */
public record DashboardResponse(
        int totalStudents,
        int confirmed,
        int absent,
        /** Contratos cancelados (assinaturas encerradas) — indicador de churn. */
        int cancelledContracts,
        List<HireRequestResponse> hireRequests,
        NoticeResponse recentNotice
) {
}
