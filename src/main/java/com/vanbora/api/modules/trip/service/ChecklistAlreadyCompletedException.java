package com.vanbora.api.modules.trip.service;

import com.vanbora.api.shared.exception.BusinessException;

/**
 * O checklist já foi concluído por outro profissional (ex.: envio simultâneo do motorista e
 * do monitor). Responde 409; o app recarrega o checklist e mostra quem concluiu e quando.
 */
public class ChecklistAlreadyCompletedException extends BusinessException {

    public ChecklistAlreadyCompletedException(String message) {
        super(message);
    }
}
