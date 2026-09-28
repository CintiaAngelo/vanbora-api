package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.Helper;

/**
 * Ajudante/monitor do transportador.
 *
 * @param hasAccess tem conta de monitor no app (só na visão do próprio transportador)
 * @param accessActive a conta de acesso está habilitada (ajudante ativo + conta ativa)
 * @param email e-mail de login do monitor (só na visão do próprio transportador)
 * @param phone telefone do monitor (só na visão do próprio transportador)
 */
public record HelperResponse(
        Long id,
        String name,
        String role,
        String photoUrl,
        boolean active,
        boolean hasAccess,
        boolean accessActive,
        String email,
        String phone
) {

    /** Visão pública (perfil do transportador para responsáveis): sem dados de acesso/contato. */
    public static HelperResponse from(Helper helper) {
        return new HelperResponse(
                helper.getId(),
                helper.getName(),
                helper.getRole(),
                helper.getPhotoUrl(),
                helper.isActive(),
                false,
                false,
                null,
                null);
    }

    /** Visão do transportador dono: inclui a conta de acesso do monitor, quando houver. */
    public static HelperResponse forOwner(Helper helper) {
        var user = helper.getUser();
        return new HelperResponse(
                helper.getId(),
                helper.getName(),
                helper.getRole(),
                helper.getPhotoUrl(),
                helper.isActive(),
                user != null,
                helper.hasActiveAccess(),
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null);
    }
}
