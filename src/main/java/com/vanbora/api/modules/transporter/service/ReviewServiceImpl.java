package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.transporter.domain.Review;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.ReviewRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação das avaliações + recálculo da média do transportador. */
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransporterProfileRepository transporterRepository;
    private final GuardianProfileRepository guardianRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             TransporterProfileRepository transporterRepository,
                             GuardianProfileRepository guardianRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.reviewRepository = reviewRepository;
        this.transporterRepository = transporterRepository;
        this.guardianRepository = guardianRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public void createReview(Long userId, Long transporterId, int rating, String comment) {
        GuardianProfile guardian = guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
        TransporterProfile transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transportador", transporterId));
        if (!enrollmentRepository.existsByDependentGuardianIdAndTransporterId(
                guardian.getId(), transporter.getId())) {
            throw new BusinessException("Você só pode avaliar um transportador que contratou.");
        }
        addReview(guardian, transporter, rating, comment);
    }

    @Override
    @Transactional
    public void addReview(GuardianProfile guardian, TransporterProfile transporter, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("A nota deve ser de 1 a 5 estrelas.");
        }
        Review review = new Review();
        review.setTransporter(transporter);
        review.setGuardian(guardian);
        review.setAuthorName(guardian.getUser().getName());
        review.setRating(rating);
        review.setComment(comment != null && !comment.isBlank() ? comment.trim() : null);
        reviewRepository.save(review);
        recompute(transporter);
    }

    @Override
    @Transactional
    public void recompute(TransporterProfile transporter) {
        List<Review> reviews = reviewRepository.findByTransporterId(transporter.getId());
        int count = reviews.size();
        BigDecimal avg = BigDecimal.ZERO;
        if (count > 0) {
            int sum = reviews.stream().mapToInt(Review::getRating).sum();
            avg = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        transporter.setRatingAvg(avg);
        transporter.setReviewsCount(count);
        transporterRepository.save(transporter);
    }
}
