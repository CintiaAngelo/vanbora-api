package com.vanbora.api.modules.schoolportal.dto;

import com.vanbora.api.modules.school.domain.School;
import org.springframework.util.StringUtils;

/** Perfil da escola autenticada no painel de gestão. */
public record SchoolProfileResponse(
        Long id,
        String name,
        String address,
        String phone,
        String email
) {
    public static SchoolProfileResponse from(School school) {
        return new SchoolProfileResponse(
                school.getId(),
                school.getName(),
                formatAddress(school),
                school.getUser() != null ? school.getUser().getPhone() : null,
                school.getUser() != null ? school.getUser().getEmail() : null);
    }

    private static String formatAddress(School s) {
        StringBuilder sb = new StringBuilder();
        append(sb, s.getStreet());
        if (StringUtils.hasText(s.getNumber())) {
            sb.append(sb.length() > 0 ? ", " : "").append(s.getNumber());
        }
        append(sb, s.getNeighborhood());
        append(sb, s.getCity());
        if (StringUtils.hasText(s.getUf())) {
            sb.append(sb.length() > 0 ? " - " : "").append(s.getUf());
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (StringUtils.hasText(part)) {
            sb.append(sb.length() > 0 ? ", " : "").append(part.trim());
        }
    }
}
