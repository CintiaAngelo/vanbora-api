package com.vanbora.api.shared.exception;

/** Violação de uma regra de negócio (HTTP 409/400). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
