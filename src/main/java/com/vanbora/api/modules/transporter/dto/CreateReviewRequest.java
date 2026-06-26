package com.vanbora.api.modules.transporter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Avaliação enviada pelo responsável (1 a 5 estrelas + comentário opcional). */
public record CreateReviewRequest(
        @Min(value = 1, message = "A nota deve ser de 1 a 5 estrelas")
        @Max(value = 5, message = "A nota deve ser de 1 a 5 estrelas")
        int rating,
        @Size(max = 500) String comment
) {
}
