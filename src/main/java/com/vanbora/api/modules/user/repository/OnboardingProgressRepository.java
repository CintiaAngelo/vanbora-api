package com.vanbora.api.modules.user.repository;

import com.vanbora.api.modules.user.domain.OnboardingProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingProgressRepository extends JpaRepository<OnboardingProgress, Long> {

    Optional<OnboardingProgress> findByUserId(Long userId);
}
