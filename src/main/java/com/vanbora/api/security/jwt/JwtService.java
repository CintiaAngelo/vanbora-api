package com.vanbora.api.security.jwt;

import com.vanbora.api.modules.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Emissão e validação de tokens JWT.
 * Responsabilidade única: lidar com o ciclo de vida do token (SRP).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = properties.expirationMillis();
    }

    /**
     * Gera um token contendo o e-mail (subject), o id e o papel do usuário, além de
     * um identificador único (jti) — é o jti que permite revogar exatamente este
     * token no logout, sem derrubar as outras sessões do mesmo usuário.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    /** Identificador único do token (claim {@code jti}), usado na revogação. */
    public String extractTokenId(String token) {
        return parse(token).getId();
    }

    /** Momento de expiração do token — define até quando vale guardá-lo na denylist. */
    public Instant extractExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
