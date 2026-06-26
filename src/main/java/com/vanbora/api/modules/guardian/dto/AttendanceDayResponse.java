package com.vanbora.api.modules.guardian.dto;

/** Um dia letivo na tela "Avisar falta". */
public record AttendanceDayResponse(
        String date,
        String weekdayLabel,
        boolean going,
        boolean today,
        boolean editable
) {
}
