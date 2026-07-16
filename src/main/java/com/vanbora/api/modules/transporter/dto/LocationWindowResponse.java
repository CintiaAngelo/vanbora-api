package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.LocationShareWindow;

/** Janela de compartilhamento (horas em "HH:mm"). */
public record LocationWindowResponse(
        int dayOfWeek,
        String startTime,
        String endTime
) {
    public static LocationWindowResponse from(LocationShareWindow w) {
        return new LocationWindowResponse(
                w.getDayOfWeek(),
                w.getStartTime().toString(),
                w.getEndTime().toString());
    }
}
