package com.vanbora.api.modules.schoolportal.dto;

/** Um aluno vinculado à escola autenticada, com responsável, transportador e presença de hoje. */
public record StudentDetailResponse(
        Long id,
        String name,
        String photoUrl,
        Long guardianId,
        String guardianName,
        String guardianPhone,
        Long transporterId,
        String transporterName,
        String neighborhood,
        String statusToday // "confirmed" | "absent"
) {
}
