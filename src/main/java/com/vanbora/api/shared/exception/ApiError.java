package com.vanbora.api.shared.exception;

import java.time.Instant;
import java.util.List;

/**
 * Corpo de resposta padronizado para erros da API.
 *
 * @param timestamp momento do erro
 * @param status    código HTTP
 * @param error     descrição curta do status
 * @param message   mensagem amigável
 * @param path      caminho da requisição
 * @param details   erros de validação por campo (opcional)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> details
) {
    public record FieldErrorDetail(String field, String message) {
    }
}
