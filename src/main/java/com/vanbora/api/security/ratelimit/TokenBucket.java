package com.vanbora.api.security.ratelimit;

import java.time.Duration;

/**
 * Token bucket: a cesta começa cheia com {@code capacity} fichas e se reenche de
 * forma contínua ao longo de um minuto. Cada requisição consome uma ficha; sem
 * ficha disponível, a requisição é recusada.
 *
 * Escolhido em vez de uma janela fixa ("N por minuto, zera às 00s") porque a janela
 * fixa permite o dobro do limite na virada — 2N requisições nos segundos ao redor
 * do corte. O bucket também absorve rajadas curtas legítimas (o app dispara várias
 * chamadas ao abrir uma tela) sem liberar tráfego sustentado acima do limite.
 *
 * Instância acessada por várias threads: todos os métodos são sincronizados. O custo
 * é irrelevante perto do I/O da requisição, e cada IP tem a sua própria cesta.
 */
final class TokenBucket {

    private static final long NANOS_PER_MINUTE = Duration.ofMinutes(1).toNanos();

    private final int capacity;
    /** Fichas disponíveis, em nanossegundos de "crédito" para evitar aritmética fracionária. */
    private double tokens;
    private long lastRefillNanos;

    TokenBucket(int capacity, long nowNanos) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
    }

    /** Consome uma ficha. Devolve {@code true} se havia crédito disponível. */
    synchronized boolean tryConsume(long nowNanos) {
        refill(nowNanos);
        if (tokens < 1.0) {
            return false;
        }
        tokens -= 1.0;
        return true;
    }

    /** Segundos até a próxima ficha ficar disponível (mínimo 1, para o Retry-After). */
    synchronized long secondsUntilNextToken(long nowNanos) {
        refill(nowNanos);
        if (tokens >= 1.0) {
            return 0;
        }
        double missing = 1.0 - tokens;
        double nanosPerToken = (double) NANOS_PER_MINUTE / capacity;
        return Math.max(1L, (long) Math.ceil(missing * nanosPerToken / 1_000_000_000d));
    }

    /** true se a cesta está cheia há mais de um minuto — pode ser descartada da memória. */
    synchronized boolean isIdle(long nowNanos) {
        return tokens >= capacity && nowNanos - lastRefillNanos > NANOS_PER_MINUTE;
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        lastRefillNanos = nowNanos;
        tokens = Math.min(capacity, tokens + (double) elapsed * capacity / NANOS_PER_MINUTE);
    }
}
