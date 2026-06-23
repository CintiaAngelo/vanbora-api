package com.vanbora.api.modules.auth.dto;

/** Resposta de autenticação contendo o token e os dados do usuário. */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
