package com.vanbora.api.shared.exception;

/** Bloqueio temporário por excesso de tentativas de login (HTTP 429). */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
