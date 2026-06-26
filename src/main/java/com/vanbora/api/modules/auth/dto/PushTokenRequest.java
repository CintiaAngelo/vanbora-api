package com.vanbora.api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Registro do token de push (Expo) de um aparelho. */
public record PushTokenRequest(
        @NotBlank String token,
        String platform
) {
}
