package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.modules.hire.domain.HireRequest;
import java.time.Instant;

/**
 * Perfil do responsável exibido ao transportador ao receber uma solicitação:
 * dados de contato, desde quando está no app, bairro de embarque, escola de
 * destino, foto e biografia.
 */
public record GuardianForTransporterResponse(
        Long guardianId,
        String name,
        String email,
        String phone,
        String photoUrl,
        String bio,
        Instant memberSince,
        String pickupNeighborhood,
        String studentName,
        String dropoffSchool,
        int dependentsCount
) {
    public static GuardianForTransporterResponse from(HireRequest h, int dependentsCount) {
        return new GuardianForTransporterResponse(
                h.getGuardian().getId(),
                h.getGuardian().getUser().getName(),
                h.getGuardian().getUser().getEmail(),
                h.getGuardian().getUser().getPhone(),
                h.getGuardian().getPhotoUrl(),
                h.getGuardian().getBio(),
                h.getGuardian().getUser().getCreatedAt(),
                h.getGuardian().getNeighborhood(),
                h.getDependent().getName(),
                h.getDependent().getSchool(),
                dependentsCount);
    }
}
