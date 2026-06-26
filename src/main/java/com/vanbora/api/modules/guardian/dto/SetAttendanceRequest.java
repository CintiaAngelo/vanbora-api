package com.vanbora.api.modules.guardian.dto;

/** Define se o aluno vai (true) ou não vai / falta (false) em uma data. */
public record SetAttendanceRequest(boolean going) {
}
