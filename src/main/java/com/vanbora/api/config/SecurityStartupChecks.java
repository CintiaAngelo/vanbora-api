package com.vanbora.api.config;

import com.vanbora.api.security.jwt.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Validações de segurança na inicialização. Falha rápido (não sobe a aplicação)
 * em vez de rodar silenciosamente em um estado inseguro.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityStartupChecks {

    static final String DEFAULT_JWT_SECRET =
            "vanbora-dev-secret-change-me-please-0123456789-abcdefghij";

    public SecurityStartupChecks(JwtProperties jwtProperties,
                                  @Value("${vanbora.environment:development}") String environment) {
        boolean production = "production".equalsIgnoreCase(environment);
        boolean defaultSecret = DEFAULT_JWT_SECRET.equals(jwtProperties.secret());
        if (production && defaultSecret) {
            throw new IllegalStateException(
                    "VANBORA_JWT_SECRET não foi definido: a API não pode iniciar em "
                            + "produção (vanbora.environment=production) usando o secret "
                            + "de desenvolvimento padrão.");
        }
    }
}
