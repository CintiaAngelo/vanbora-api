package com.vanbora.api.modules.trip.dto;

import java.time.Instant;

/**
 * Conferência de segurança concluída num percurso do filho, vista pelo responsável.
 * Não inclui as fotos (mostram o interior da van e podem expor outras crianças).
 */
public record GuardianSafetyCheckResponse(
        Long id,
        String tripName,
        Instant tripFinishedAt,
        Instant completedAt,
        String completedByName,
        String completedByRole
) {
}
