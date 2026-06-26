package com.vanbora.api.modules.auth.dto;

import java.time.Instant;

/** Situação do consentimento (LGPD) do usuário autenticado. */
public record ConsentStatusResponse(
        /** true = consentimento vigente (aceito e não revogado). */
        boolean granted,
        String version,
        Instant acceptedAt,
        Instant revokedAt
) {
}
