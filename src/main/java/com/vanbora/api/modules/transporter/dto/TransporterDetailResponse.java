package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.math.BigDecimal;
import java.util.List;

/** Perfil público completo do transportador, com foto, ajudantes e avaliações. */
public record TransporterDetailResponse(
        Long id,
        String name,
        String photoUrl,
        String bio,
        String phone,
        BigDecimal rating,
        int reviewsCount,
        int yearsExperience,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal monthlyFee,
        boolean acceptsProposals,
        List<HelperResponse> helpers,
        List<ReviewResponse> reviews,
        List<String> vehiclePhotoUrls,
        List<String> vehicleCharacteristics,
        List<String> vehicleAccessibilityFeatures
) {
    public static TransporterDetailResponse from(
            TransporterProfile t, List<HelperResponse> helpers, List<ReviewResponse> reviews) {
        return new TransporterDetailResponse(
                t.getId(),
                t.getUser().getName(),
                t.getPhotoUrl(),
                t.getBio(),
                t.getUser().getPhone(),
                t.getRatingAvg(),
                t.getReviewsCount() == null ? 0 : t.getReviewsCount(),
                t.getYearsExperience() == null ? 0 : t.getYearsExperience(),
                List.copyOf(t.getSchools()),
                List.copyOf(t.getNeighborhoods()),
                t.getBaseMonthlyFee(),
                t.isAcceptsProposals(),
                helpers,
                reviews,
                List.copyOf(t.getVehiclePhotoUrls()),
                t.getVehicleCharacteristics().stream().map(Enum::name).toList(),
                t.getVehicleAccessibilityFeatures().stream().map(Enum::name).toList());
    }
}
