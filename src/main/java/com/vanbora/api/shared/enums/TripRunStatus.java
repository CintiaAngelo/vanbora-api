package com.vanbora.api.shared.enums;

/** Estado de uma execução de percurso (um trajeto realizado num dia). */
public enum TripRunStatus {
    /** Iniciado e ainda não encerrado. */
    IN_PROGRESS,
    /** Encerrado normalmente — gera o checklist de segurança pós-percurso. */
    COMPLETED,
    /** Cancelado antes de terminar (não ocorreu) — não gera checklist. */
    CANCELLED
}
