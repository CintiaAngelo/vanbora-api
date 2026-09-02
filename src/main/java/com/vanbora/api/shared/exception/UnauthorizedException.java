package com.vanbora.api.shared.exception;

/**
 * Credencial ausente, inválida ou expirada — resulta em HTTP 401.
 *
 * Diferente de {@link BusinessException} (409), que indica uma regra de negócio
 * violada com o usuário já identificado.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
