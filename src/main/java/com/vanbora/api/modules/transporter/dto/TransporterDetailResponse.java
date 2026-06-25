package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.math.BigDecimal;
import java.util.List;

/** Perfil público completo do transportador, com foto, ajudantes e avaliações. */
public record TransporterDetailResponse(
        Long id,
        String name,
        String photoUrl,
        String phone,
        BigDecimal rating,
        int reviewsCount,
        int yearsExperience,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal monthlyFee,
        boolean acceptsProposals,
        int availableSeats,
        List<HelperResponse> helpers,
        List<ReviewResponse> reviews
) {
    public static TransporterDetailResponse from(
            TransporterProfile t, List<HelperResponse> helpers, List<ReviewResponse> reviews) {
        return new TransporterDetailResponse(
                t.getId(),
                t.getUser().getName(),
                t.getPhotoUrl(),
                t.getUser().getPhone(),
                t.getRatingAvg(),
                t.getReviewsCount() == null ? 0 : t.getReviewsCount(),
                t.getYearsExperience() == null ? 0 : t.getYearsExperience(),
                List.copyOf(t.getSchools()),
                List.copyOf(t.getNeighborhoods()),
                t.getBaseMonthlyFee(),
                t.isAcceptsProposals(),
                t.getAvailableSeats() == null ? 0 : t.getAvailableSeats(),
                helpers,
                reviews);
    }
}
