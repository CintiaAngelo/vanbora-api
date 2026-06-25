package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.time.Instant;

/** Última posição conhecida do transportador. */
public record TransporterLocationResponse(
        Double latitude,
        Double longitude,
        Instant updatedAt
) {
    public static TransporterLocationResponse from(TransporterProfile transporter) {
        return new TransporterLocationResponse(
                transporter.getCurrentLatitude(),
                transporter.getCurrentLongitude(),
                transporter.getLocationUpdatedAt());
    }
}
