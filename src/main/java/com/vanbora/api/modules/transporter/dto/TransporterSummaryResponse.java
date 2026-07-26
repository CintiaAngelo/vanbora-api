package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.math.BigDecimal;
import java.util.List;

/** Resumo do transportador nos resultados de busca. */
public record TransporterSummaryResponse(
        Long id,
        String name,
        String photoUrl,
        BigDecimal rating,
        int reviewsCount,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal monthlyFee,
        /** true = atende a escola/bairro buscado (aparece na seção "perto de você"). */
        boolean nearby
) {
    public static TransporterSummaryResponse from(TransporterProfile t, boolean nearby) {
        return new TransporterSummaryResponse(
                t.getId(),
                t.getUser().getName(),
                t.getPhotoUrl(),
                t.getRatingAvg(),
                t.getReviewsCount() == null ? 0 : t.getReviewsCount(),
                List.copyOf(t.getSchools()),
                List.copyOf(t.getNeighborhoods()),
                t.getBaseMonthlyFee(),
                nearby);
    }
}
