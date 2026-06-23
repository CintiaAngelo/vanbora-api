package com.vanbora.api.modules.guardian.repository;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianProfileRepository extends JpaRepository<GuardianProfile, Long> {

    Optional<GuardianProfile> findByUserId(Long userId);
}
