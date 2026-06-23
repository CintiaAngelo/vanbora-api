package com.vanbora.api.shared.exception;

/** Lançada quando um recurso solicitado não existe (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s não encontrado(a): %s".formatted(resource, id));
    }
}
