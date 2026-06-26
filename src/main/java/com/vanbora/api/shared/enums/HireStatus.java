package com.vanbora.api.shared.enums;

/** Estado de uma solicitação de contratação. */
public enum HireStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    /** Cancelada pelo responsável antes de o transportador responder. */
    CANCELLED,
    /** Expirada automaticamente (transportador não respondeu no prazo). */
    EXPIRED
}
