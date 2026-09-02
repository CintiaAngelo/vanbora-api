package com.vanbora.api.modules.auth.social;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do login social via Auth0 (Google e Facebook).
 *
 * Todos os valores aqui são públicos: domínio do tenant e client id da aplicação
 * nativa. O fluxo usado no app é o Authorization Code com PKCE, próprio para
 * clientes públicos — ele não usa client secret, e nenhum secret do Auth0 deve
 * ser guardado no app nem nesta API.
 *
 * @param domain              domínio do tenant (ex.: {@code vanbora.us.auth0.com});
 *                            vazio desliga o recurso
 * @param audience            client id(s) aceitos no id_token, separados por vírgula
 * @param jwksCacheMinutes    validade do cache das chaves públicas do Auth0
 * @param signupTicketMinutes validade do ticket devolvido quando o e-mail social
 *                            ainda não tem conta no VanBora
 */
@ConfigurationProperties(prefix = "vanbora.auth0")
public record Auth0Properties(
        String domain,
        String audience,
        long jwksCacheMinutes,
        long signupTicketMinutes
) {

    /** true quando o tenant está configurado; caso contrário o login social fica off. */
    public boolean isEnabled() {
        return domain != null && !domain.isBlank();
    }

    /** Emissor esperado no id_token — o Auth0 sempre inclui a barra final. */
    public String issuer() {
        return "https://" + domain.replaceAll("^https?://", "").replaceAll("/+$", "") + "/";
    }

    public String jwksUrl() {
        return issuer() + ".well-known/jwks.json";
    }

    public List<String> audiences() {
        if (audience == null || audience.isBlank()) {
            return List.of();
        }
        return Arrays.stream(audience.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
