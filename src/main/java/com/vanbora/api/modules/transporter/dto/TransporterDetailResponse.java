package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.math.BigDecimal;
import java.util.List;

/** Perfil público completo do transportador, com avaliações. */
public record TransporterDetailResponse(
        Long id,
        String name,
        BigDecimal rating,
        int reviewsCount,
        int yearsExperience,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal monthlyFee,
        int availableSeats,
        List<ReviewResponse> reviews
) {
    public static TransporterDetailResponse from(TransporterProfile t, List<ReviewResponse> reviews) {
        return new TransporterDetailResponse(
                t.getId(),
                t.getUser().getName(),
                t.getRatingAvg(),
                t.getReviewsCount() == null ? 0 : t.getReviewsCount(),
                t.getYearsExperience() == null ? 0 : t.getYearsExperience(),
                List.copyOf(t.getSchools()),
                List.copyOf(t.getNeighborhoods()),
                t.getBaseMonthlyFee(),
                t.getAvailableSeats() == null ? 0 : t.getAvailableSeats(),
                reviews);
    }
}
