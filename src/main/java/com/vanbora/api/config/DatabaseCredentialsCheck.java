package com.vanbora.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Falha cedo, e com uma mensagem útil, quando a senha do banco não foi configurada.
 *
 * Roda como {@link EnvironmentPostProcessor} — antes de qualquer bean, e portanto
 * antes de o Hibernate tentar conectar. Sem isso, o que aparece é um
 * {@code Access denied for user 'vanbora'@'localhost' (using password: NO)} no meio
 * de centenas de linhas de stack trace, que não diz o que fazer.
 *
 * Diferente das checagens de {@link SecurityStartupChecks}, esta é fatal em qualquer
 * ambiente: não é um alerta de segurança que pode virar aviso em desenvolvimento —
 * sem senha a aplicação simplesmente não funciona.
 *
 * Registrado em {@code META-INF/spring.factories}.
 */
public class DatabaseCredentialsCheck implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        String url = environment.getProperty("spring.datasource.url", "");
        // O banco em memória dos testes não usa senha.
        if (url.startsWith("jdbc:h2:")) {
            return;
        }
        String password = environment.getProperty("spring.datasource.password", "");
        if (!password.isBlank()) {
            return;
        }

        throw new IllegalStateException(String.join(System.lineSeparator(),
                "",
                "A senha do banco de dados não foi configurada.",
                "",
                "Ela não fica no application.yml de propósito: o arquivo é versionado, e",
                "uma senha ali vaza junto com o repositório. Escolha uma das opções:",
                "",
                "  1) Crie o arquivo backend/.env (ignorado pelo git) com uma linha:",
                "         DB_PASSWORD=sua_senha",
                "     Copie backend/.env.example como ponto de partida. A API lê esse",
                "     arquivo sozinha — funciona no IntelliJ e no mvnw sem mais nada.",
                "",
                "  2) Ou defina a variável de ambiente DB_PASSWORD:",
                "         PowerShell:  $env:DB_PASSWORD = \"sua_senha\"",
                "         IntelliJ:    Run > Edit Configurations > Environment variables",
                ""));
    }
}
