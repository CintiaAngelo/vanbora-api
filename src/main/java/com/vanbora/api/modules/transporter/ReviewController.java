package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.transporter.dto.CreateReviewRequest;
import com.vanbora.api.modules.transporter.service.ReviewService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Avaliações criadas pelo responsável sobre um transportador. */
@RestController
@RequestMapping("/api/guardians/me")
@PreAuthorize("hasRole('GUARDIAN')")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    public ReviewController(ReviewService reviewService, CurrentUserProvider currentUserProvider) {
        this.reviewService = reviewService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/transporters/{transporterId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long transporterId, @Valid @RequestBody CreateReviewRequest request) {
        reviewService.createReview(
                currentUserProvider.requireUserId(), transporterId, request.rating(), request.comment());
    }
}
