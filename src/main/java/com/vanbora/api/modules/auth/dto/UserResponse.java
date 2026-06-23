package com.vanbora.api.modules.auth.dto;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.UserRole;

/** Representação pública do usuário autenticado. */
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }
}
