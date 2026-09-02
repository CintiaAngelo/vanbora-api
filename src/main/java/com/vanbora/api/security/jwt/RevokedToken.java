package com.vanbora.api.security.jwt;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token invalidado antes de expirar (logout).
 *
 * JWT é auto-contido: uma vez emitido, vale até a data de expiração mesmo que o
 * app apague sua cópia. Sem esta lista, um token copiado do aparelho continuaria
 * aceito por até 24h depois do usuário sair da conta.
 *
 * Fica no banco (e não só em memória) para sobreviver a um restart da API — do
 * contrário, reiniciar o serviço "ressuscitaria" todos os tokens deslogados.
 * Registros vencidos são apagados periodicamente por {@link TokenRevocationService}.
 */
@Entity
@Table(
        name = "revoked_tokens",
        indexes = {
                @Index(name = "idx_revoked_tokens_token_id", columnList = "token_id", unique = true),
                @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class RevokedToken extends BaseEntity {

    /** Claim {@code jti} do token revogado. */
    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    /** Expiração original do token: depois dela o registro é descartável. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "user_id")
    private Long userId;
}
