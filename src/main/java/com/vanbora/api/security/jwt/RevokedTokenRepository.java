package com.vanbora.api.security.jwt;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);

    /** Tokens revogados que ainda não venceram — usado para reidratar o cache no boot. */
    List<RevokedToken> findByExpiresAtAfter(Instant moment);

    @Modifying
    @Query("delete from RevokedToken r where r.expiresAt < :moment")
    int deleteExpired(@Param("moment") Instant moment);
}
