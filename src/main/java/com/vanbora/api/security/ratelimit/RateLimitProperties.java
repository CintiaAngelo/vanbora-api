package com.vanbora.api.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do rate limiting por IP (ver {@link RateLimitFilter}).
 *
 * @param enabled                  liga/desliga o filtro por completo
 * @param authRequestsPerMinute    limite para /api/auth/** (login, cadastro, social)
 * @param writeRequestsPerMinute   limite para POST/PUT/PATCH/DELETE no restante da API
 * @param defaultRequestsPerMinute limite para as demais requisições
 * @param trustForwardedFor        usar X-Forwarded-For como IP do cliente; só ligue
 *                                 atrás de um proxy reverso confiável, senão o
 *                                 cabeçalho é forjável e o limite deixa de valer
 */
@ConfigurationProperties(prefix = "vanbora.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int authRequestsPerMinute,
        int writeRequestsPerMinute,
        int defaultRequestsPerMinute,
        boolean trustForwardedFor
) {
}
