package com.vanbora.api.security.jwt;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista de tokens invalidados antes do vencimento (logout).
 *
 * O cache em memória evita uma consulta ao banco a cada requisição autenticada;
 * o banco é a fonte da verdade e repovoa o cache na inicialização.
 */
@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository repository;
    /** jti -> expiração do token. */
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    /** Recarrega do banco os tokens revogados ainda válidos (sobrevive a restart). */
    @PostConstruct
    @Transactional(readOnly = true)
    public void loadFromDatabase() {
        try {
            repository.findByExpiresAtAfter(Instant.now())
                    .forEach(entry -> revoked.put(entry.getTokenId(), entry.getExpiresAt()));
            log.info("Tokens revogados carregados: {}", revoked.size());
        } catch (Exception ex) {
            // Tabela ainda não criada na primeiríssima subida, por exemplo. Não é
            // motivo para impedir a API de iniciar: o pior caso é um token deslogado
            // continuar aceito até expirar.
            log.warn("Não foi possível carregar a lista de tokens revogados: {}", ex.getMessage());
        }
    }

    /** Invalida o token informado até a sua expiração original. */
    @Transactional
    public void revoke(String tokenId, Instant expiresAt, Long userId) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            // Token emitido por uma versão anterior da API, sem jti: nada a revogar.
            return;
        }
        revoked.put(tokenId, expiresAt);
        if (repository.existsByTokenId(tokenId)) {
            return;
        }
        RevokedToken entry = new RevokedToken();
        entry.setTokenId(tokenId);
        entry.setExpiresAt(expiresAt);
        entry.setUserId(userId);
        repository.save(entry);
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        Instant expiresAt = revoked.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            // Já venceu por conta própria; a validação de exp do JWT cuida do resto.
            revoked.remove(tokenId);
            return false;
        }
        return true;
    }

    /** Limpeza diária: um token vencido já é recusado pela própria validação do JWT. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = repository.deleteExpired(now);
        if (removed > 0) {
            log.info("Tokens revogados expirados removidos: {}", removed);
        }
    }
}
