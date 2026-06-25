package com.vanbora.api.modules.notice.dto;

/** Possível destinatário de um aviso (derivado das matrículas ativas). */
public record AudienceRecipientResponse(
        Long guardianId,
        String guardianName,
        String studentName,
        String school,
        String neighborhood
) {
}
