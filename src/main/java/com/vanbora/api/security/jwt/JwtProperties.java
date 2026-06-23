package com.vanbora.api.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do JWT (lidas de application.yml / variáveis de ambiente).
 *
 * @param secret           chave secreta (Base64 ou texto longo) para assinar tokens
 * @param expirationMillis validade do token em milissegundos
 */
@ConfigurationProperties(prefix = "vanbora.jwt")
public record JwtProperties(String secret, long expirationMillis) {
}
