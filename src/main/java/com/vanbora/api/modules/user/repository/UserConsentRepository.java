package com.vanbora.api.modules.user.repository;

import com.vanbora.api.modules.user.domain.UserConsent;
import com.vanbora.api.shared.enums.ConsentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    List<UserConsent> findByUserId(Long userId);

    /** Consentimento mais recente do usuário para o tipo informado. */
    Optional<UserConsent> findFirstByUserIdAndTypeOrderByIdDesc(Long userId, ConsentType type);
}
