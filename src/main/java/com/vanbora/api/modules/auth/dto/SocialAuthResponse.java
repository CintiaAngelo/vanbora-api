package com.vanbora.api.modules.auth.dto;

/**
 * Resultado do login social.
 *
 * São dois desfechos possíveis:
 *   - {@code registered = true}: já existe conta com aquele e-mail, então
 *     {@code session} vem preenchida e o app entra direto;
 *   - {@code registered = false}: primeiro acesso. O app segue para a escolha de
 *     perfil e o cadastro, usando {@code profile} para preencher os campos e
 *     {@code signupTicket} para dispensar a criação de senha.
 *
 * @param registered   se o e-mail social já tem conta no VanBora
 * @param session      token + usuário (apenas quando {@code registered})
 * @param profile      nome/e-mail/provedor vindos da conta social
 * @param signupTicket prova temporária do login social (apenas quando não registrado)
 */
public record SocialAuthResponse(
        boolean registered,
        AuthResponse session,
        SocialProfile profile,
        String signupTicket
) {

    /** Dados públicos trazidos do provedor social, para preencher o cadastro. */
    public record SocialProfile(String email, String name, String provider) {
    }

    public static SocialAuthResponse loggedIn(AuthResponse session, SocialProfile profile) {
        return new SocialAuthResponse(true, session, profile, null);
    }

    public static SocialAuthResponse needsSignup(SocialProfile profile, String signupTicket) {
        return new SocialAuthResponse(false, null, profile, signupTicket);
    }
}
