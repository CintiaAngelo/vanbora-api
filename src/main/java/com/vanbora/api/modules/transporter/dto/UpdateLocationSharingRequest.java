package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.Valid;
import java.util.List;

/** Atualização do compartilhamento: interruptor mestre + janelas agendadas. */
public record UpdateLocationSharingRequest(
        boolean enabled,
        @Valid List<LocationWindowInput> windows
) {
}
