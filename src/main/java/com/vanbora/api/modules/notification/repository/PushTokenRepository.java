package com.vanbora.api.modules.notification.repository;

import com.vanbora.api.modules.notification.domain.PushToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUserId(Long userId);

    Optional<PushToken> findByToken(String token);

    @Transactional
    void deleteByTokenAndUserId(String token, Long userId);
}
