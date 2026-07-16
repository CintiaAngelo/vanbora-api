package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.util.List;

/** Configuração de compartilhamento de localização do transportador. */
public record LocationSharingResponse(
        boolean enabled,
        List<LocationWindowResponse> windows
) {
    public static LocationSharingResponse from(TransporterProfile t) {
        return new LocationSharingResponse(
                t.isLocationSharingEnabled(),
                t.getLocationShareWindows().stream()
                        .map(LocationWindowResponse::from)
                        .toList());
    }
}
