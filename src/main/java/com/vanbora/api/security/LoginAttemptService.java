package com.vanbora.api.security;

import com.vanbora.api.shared.exception.TooManyAttemptsException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proteção contra força bruta no login: bloqueia temporariamente uma conta após
 * um número de tentativas malsucedidas consecutivas.
 *
 * Implementação em memória (adequada para uma única instância da API; se a API
 * rodar com múltiplas réplicas no futuro, isso precisará migrar para um contador
 * compartilhado, ex.: Redis).
 */
@Component
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockoutDuration;
    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${vanbora.login.max-attempts:5}") int maxAttempts,
            @Value("${vanbora.login.lockout-minutes:15}") long lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
    }

    /** Lança TooManyAttemptsException se a conta estiver temporariamente bloqueada. */
    public void checkAllowed(String email) {
        Attempts attempts = attemptsByEmail.get(normalize(email));
        if (attempts != null && attempts.isLocked(lockoutDuration)) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(normalize(email), (key, current) -> {
            Attempts attempts = (current == null || current.isExpired(lockoutDuration))
                    ? new Attempts()
                    : current;
            attempts.count++;
            attempts.lastFailureAt = Instant.now();
            return attempts;
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private class Attempts {
        private int count = 0;
        private Instant lastFailureAt = Instant.now();

        boolean isLocked(Duration lockout) {
            return count >= maxAttempts && !isExpired(lockout);
        }

        boolean isExpired(Duration lockout) {
            return Instant.now().isAfter(lastFailureAt.plus(lockout));
        }
    }
}
