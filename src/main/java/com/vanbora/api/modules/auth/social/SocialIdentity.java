package com.vanbora.api.modules.auth.social;

/**
 * Identidade extraída de um id_token do Auth0 já verificado.
 *
 * @param subject       identificador estável no Auth0 (ex.: {@code google-oauth2|1234})
 * @param provider      provedor legível: {@code google}, {@code facebook}, ...
 * @param email         e-mail da conta social
 * @param emailVerified se o provedor confirmou o e-mail
 * @param name          nome exibido, quando informado
 */
public record SocialIdentity(
        String subject,
        String provider,
        String email,
        boolean emailVerified,
        String name
) {

    /**
     * Deriva o provedor a partir do {@code sub}, que o Auth0 monta como
     * {@code <connection>|<id no provedor>}.
     */
    public static String providerFromSubject(String subject) {
        if (subject == null || !subject.contains("|")) {
            return "auth0";
        }
        String connection = subject.substring(0, subject.indexOf('|')).toLowerCase();
        if (connection.startsWith("google")) {
            return "google";
        }
        if (connection.startsWith("facebook")) {
            return "facebook";
        }
        return connection;
    }
}
