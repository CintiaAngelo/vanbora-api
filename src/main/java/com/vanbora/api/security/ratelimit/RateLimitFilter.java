package com.vanbora.api.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanbora.api.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting por IP de origem, aplicado antes da autenticação.
 *
 * Três faixas, porque o risco de cada grupo de rotas é diferente:
 *   - AUTH  (/api/auth/**): tentativas de login/cadastro. É a faixa mais estreita.
 *   - WRITE (POST/PUT/PATCH/DELETE): escrita, mais cara e com efeito colateral.
 *   - READ  (o resto): leitura, onde o app naturalmente faz mais chamadas.
 *
 * Complementa — não substitui — o {@code LoginAttemptService}, que bloqueia UMA
 * conta após N senhas erradas. Aquele não impede *password spraying* (uma senha
 * comum testada em milhares de e-mails diferentes, sem estourar o contador de
 * nenhuma conta); este impede, porque conta por origem, não por conta.
 *
 * Estado em memória: vale para uma instância da API. Com múltiplas réplicas, cada
 * uma aplica o próprio limite — para um limite global, o contador precisa migrar
 * para um armazenamento compartilhado (Redis), como já anotado no LoginAttemptService.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Requisições entre varreduras de limpeza das cestas ociosas. */
    private static final int CLEANUP_INTERVAL = 1_000;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Faixas de limite, da mais restrita para a mais permissiva. */
    private enum Tier { AUTH, WRITE, READ }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        // Preflight do CORS não é tráfego de aplicação e não deve consumir ficha.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // Imagens estáticas e o handshake do WebSocket ficam de fora: a tela de rota
        // carrega várias fotos de uma vez e o socket reconecta sozinho — limitar isso
        // quebraria uso legítimo sem reduzir risco.
        return path.startsWith("/uploads/")
                || path.startsWith("/ws/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Tier tier = resolveTier(request);
        String key = tier.name() + '|' + clientIp(request);
        long now = System.nanoTime();

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limitFor(tier), now));

        if (!bucket.tryConsume(now)) {
            writeTooManyRequests(request, response, bucket.secondsUntilNextToken(now));
            return;
        }

        maybeCleanup(now);
        filterChain.doFilter(request, response);
    }

    private Tier resolveTier(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/auth/")) {
            return Tier.AUTH;
        }
        return switch (request.getMethod().toUpperCase()) {
            case "POST", "PUT", "PATCH", "DELETE" -> Tier.WRITE;
            default -> Tier.READ;
        };
    }

    private int limitFor(Tier tier) {
        int configured = switch (tier) {
            case AUTH -> properties.authRequestsPerMinute();
            case WRITE -> properties.writeRequestsPerMinute();
            case READ -> properties.defaultRequestsPerMinute();
        };
        // Um limite <= 0 por engano na configuração deixaria a API inacessível.
        return Math.max(1, configured);
    }

    /**
     * IP do cliente. {@code X-Forwarded-For} só é considerado quando a API está
     * declaradamente atrás de um proxy confiável: o cabeçalho é enviado pelo cliente
     * e, sem proxy que o sobrescreva, qualquer um trocaria de "IP" a cada requisição
     * para nunca esbarrar no limite.
     */
    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // O primeiro endereço da lista é o cliente original.
                return forwarded.split(",")[0].trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "desconhecido";
    }

    private void writeTooManyRequests(
            HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Muitas requisições em pouco tempo. Aguarde um instante e tente de novo.",
                request.getRequestURI(),
                null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** Descarta cestas cheias e paradas, para o mapa não crescer sem limite. */
    private void maybeCleanup(long nowNanos) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        buckets.values().removeIf(bucket -> bucket.isIdle(nowNanos));
    }
}
