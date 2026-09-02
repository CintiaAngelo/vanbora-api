package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login social: o app envia o {@code id_token} obtido do Auth0 depois de entrar
 * com Google ou Facebook. A API verifica a assinatura antes de confiar no conteúdo.
 *
 * @param idToken id_token (JWT RS256) emitido pelo tenant do Auth0
 */
public record SocialLoginRequest(@NotBlank String idToken) {
}
