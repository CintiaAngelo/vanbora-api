package com.vanbora.api.shared.enums;

/**
 * Estado do checklist de segurança pós-percurso. Só existe para percursos encerrados
 * (um percurso cancelado não chega a gerar checklist), por isso não há CANCELLED aqui.
 */
public enum ChecklistStatus {
    PENDING,
    COMPLETED
}
