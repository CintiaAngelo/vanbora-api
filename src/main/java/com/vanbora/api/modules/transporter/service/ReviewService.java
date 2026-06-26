package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;

/** Avaliações dos transportadores e recálculo da média. */
public interface ReviewService {

    /** Cria uma avaliação (valida que o responsável tem/teve vínculo com o transportador). */
    void createReview(Long userId, Long transporterId, int rating, String comment);

    /** Registra a avaliação a partir das entidades já resolvidas (usado no cancelamento). */
    void addReview(GuardianProfile guardian, TransporterProfile transporter, int rating, String comment);

    /** Recalcula média (ratingAvg) e contagem (reviewsCount) a partir das avaliações reais. */
    void recompute(TransporterProfile transporter);
}
