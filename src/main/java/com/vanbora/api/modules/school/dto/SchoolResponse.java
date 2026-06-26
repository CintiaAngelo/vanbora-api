package com.vanbora.api.modules.school.dto;

import com.vanbora.api.modules.school.domain.School;
import org.springframework.util.StringUtils;

/** Escola exibida no seletor/busca do responsável. */
public record SchoolResponse(
        Long id,
        String name,
        String address,
        String city,
        Double latitude,
        Double longitude
) {
    public static SchoolResponse from(School s) {
        return new SchoolResponse(s.getId(), s.getName(), formatAddress(s), s.getCity(),
                s.getLatitude(), s.getLongitude());
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
