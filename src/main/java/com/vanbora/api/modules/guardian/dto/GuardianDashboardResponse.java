package com.vanbora.api.modules.guardian.dto;

import com.vanbora.api.modules.payment.dto.PaymentResponse;
import java.util.List;

/** Dados do painel inicial do responsável. */
public record GuardianDashboardResponse(
        boolean hasTransporter,
        String studentName,
        List<DayAttendance> weekAttendance,
        PaymentResponse nextPayment,
        TransporterNotice notice
) {
    /** Presença em um dia (rótulo SEG..SEX). */
    public record DayAttendance(String day, Boolean present) {
    }

    /** Aviso recente do transportador contratado. */
    public record TransporterNotice(String title, String message) {
    }
}
