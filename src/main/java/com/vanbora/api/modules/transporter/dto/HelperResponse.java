package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.Helper;

/** Ajudante/monitor do transportador. */
public record HelperResponse(Long id, String name, String role, String photoUrl, boolean active) {

    public static HelperResponse from(Helper helper) {
        return new HelperResponse(
                helper.getId(),
                helper.getName(),
                helper.getRole(),
                helper.getPhotoUrl(),
                helper.isActive());
    }
}
