package com.vanbora.api.shared.exception;

/**
 * Recurso opcional não configurado nesta instalação (ex.: login social sem tenant
 * do Auth0 definido) — resulta em HTTP 503.
 *
 * Separado de um erro genérico para que o app consiga distinguir "não disponível
 * aqui" de "deu errado" e simplesmente esconder o botão.
 */
public class FeatureUnavailableException extends RuntimeException {

    public FeatureUnavailableException(String message) {
        super(message);
    }
}
