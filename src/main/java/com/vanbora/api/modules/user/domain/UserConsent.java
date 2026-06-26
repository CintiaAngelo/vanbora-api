package com.vanbora.api.modules.user.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de consentimento do usuário (LGPD). Mantém um histórico auditável de
 * qual documento (e versão) foi aceito e quando — para que o consentimento seja
 * demonstrável (art. 8º §1º).
 */
@Entity
@Table(name = "user_consents")
@Getter
@Setter
@NoArgsConstructor
public class UserConsent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConsentType type;

    @Column(name = "document_version", nullable = false, length = 40)
    private String documentVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    /** Data/hora da revogação do consentimento (LGPD, art. 8º §5º). Null = vigente. */
    @Column(name = "revoked_at")
    private Instant revokedAt;
}
