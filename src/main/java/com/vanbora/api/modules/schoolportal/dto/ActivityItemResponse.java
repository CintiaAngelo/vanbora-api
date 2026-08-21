package com.vanbora.api.modules.schoolportal.dto;

import java.time.Instant;

/** Evento recente na escola: "absence" | "new_link" | "review". */
public record ActivityItemResponse(
        String type,
        String transporterName,
        String studentName,
        Instant timestamp
) {
}
