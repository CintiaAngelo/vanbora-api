package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.Review;

/** Avaliação exibida no perfil público. */
public record ReviewResponse(Long id, String authorName, int rating, String comment) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(), review.getAuthorName(), review.getRating(), review.getComment());
    }
}
