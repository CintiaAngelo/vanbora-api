package com.vanbora.api.config;

import com.vanbora.api.security.jwt.JwtProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Validações de segurança na inicialização. Falha rápido (não sobe a aplicação)
 * em vez de rodar silenciosamente em um estado inseguro.
 *
 * Em produção ({@code vanbora.environment=production}) os problemas abaixo são
 * fatais; em desenvolvimento viram apenas um aviso no log, para não atrapalhar
 * quem está rodando localmente.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityStartupChecks {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupChecks.class);

    static final String DEFAULT_JWT_SECRET =
            "vanbora-dev-secret-change-me-please-0123456789-abcdefghij";

    /** Tamanho mínimo do secret para HMAC-SHA256 (256 bits). */
    private static final int MIN_SECRET_LENGTH = 32;

    public SecurityStartupChecks(
            JwtProperties jwtProperties,
            @Value("${vanbora.environment:development}") String environment,
            @Value("${vanbora.cors.allowed-origins:}") String corsAllowedOrigins,
            @Value("${vanbora.seed-demo-data:false}") boolean seedDemoData,
            @Value("${vanbora.auth0.domain:}") String auth0Domain,
            @Value("${vanbora.auth0.audience:}") String auth0Audience) {

        boolean production = "production".equalsIgnoreCase(environment);
        List<String> problems = new ArrayList<>();

        if (DEFAULT_JWT_SECRET.equals(jwtProperties.secret())) {
            problems.add("VANBORA_JWT_SECRET não foi definido — a API está assinando tokens "
                    + "com o secret de desenvolvimento, que é público no repositório.");
        } else if (jwtProperties.secret() == null
                || jwtProperties.secret().length() < MIN_SECRET_LENGTH) {
            problems.add("VANBORA_JWT_SECRET tem menos de " + MIN_SECRET_LENGTH
                    + " caracteres — use um valor aleatório longo.");
        }

        // A ausência de DB_PASSWORD é verificada antes, em DatabaseCredentialsCheck:
        // sem ela a aplicação não sobe em ambiente nenhum, então não é um "aviso".

        if (containsBroadOrigin(corsAllowedOrigins)) {
            problems.add("vanbora.cors.allowed-origins ainda contém origens de desenvolvimento "
                    + "(localhost / faixas de rede local / curinga). Defina "
                    + "VANBORA_CORS_ALLOWED_ORIGINS com os domínios reais.");
        }

        if (seedDemoData) {
            problems.add("VANBORA_SEED=true cria usuários de demonstração com senha conhecida. "
                    + "Desligue em produção (VANBORA_SEED=false).");
        }

        // Sem audience, um id_token emitido para outro aplicativo do mesmo tenant
        // Auth0 seria aceito como login válido aqui.
        if (!auth0Domain.isBlank() && auth0Audience.isBlank()) {
            problems.add("AUTH0_DOMAIN está definido mas AUTH0_AUDIENCE não: o login social "
                    + "aceitaria tokens emitidos para outras aplicações do mesmo tenant. "
                    + "Informe o(s) client id(s) do app.");
        }

        if (problems.isEmpty()) {
            return;
        }
        if (production) {
            throw new IllegalStateException(
                    "A API não pode iniciar em produção (vanbora.environment=production):"
                            + System.lineSeparator() + "  - "
                            + String.join(System.lineSeparator() + "  - ", problems));
        }
        problems.forEach(problem -> log.warn("[segurança] {}", problem));
    }

    /** Origens aceitáveis só em desenvolvimento — nunca em produção. */
    private boolean containsBroadOrigin(String allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return false;
        }
        String value = allowedOrigins.toLowerCase();
        return value.contains("localhost")
                || value.contains("127.0.0.1")
                || value.contains("192.168.")
                || value.contains("10.*")
                || value.contains("exp://")
                || value.contains("*://")
                || value.trim().equals("*");
    }
}
