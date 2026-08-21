package com.vanbora.api.modules.schoolportal.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Um transportador que atende a escola autenticada, com as contagens do dia (visão do painel
 * de gestão). Todo transportador aqui atende ativamente a escola — não existe hoje um estado
 * "inativo" no modelo real, então {@code active} é sempre true.
 */
public record TransporterStatsResponse(
        Long id,
        String name,
        String phone,
        String email,
        String photoUrl,
        boolean active,
        Instant memberSince,
        String plate,
        List<HelperResponse> helpers,
        List<String> areas,
        BigDecimal rating,
        int reviewsCount,
        int studentsLinked,
        int confirmedToday,
        int absentToday
) {
    public static TransporterStatsResponse from(
            TransporterProfile t, int studentsLinked, int confirmedToday, int absentToday) {
        return new TransporterStatsResponse(
                t.getId(),
                t.getUser().getName(),
                t.getUser().getPhone(),
                t.getUser().getEmail(),
                t.getPhotoUrl(),
                true,
                t.getCreatedAt(),
                t.getPlate(),
                t.getHelpers().stream().map(HelperResponse::from).toList(),
                List.copyOf(t.getNeighborhoods()),
                t.getRatingAvg(),
                t.getReviewsCount() == null ? 0 : t.getReviewsCount(),
                studentsLinked,
                confirmedToday,
                absentToday);
    }
}
