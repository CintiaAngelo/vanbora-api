package com.vanbora.api.modules.hire.domain;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.HireStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solicitação de contratação enviada por um responsável a um transportador. */
@Entity
@Table(name = "hire_requests")
@Getter
@Setter
@NoArgsConstructor
public class HireRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_id", nullable = false)
    private Dependent dependent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HireStatus status = HireStatus.PENDING;

    /** Valor proposto pelo responsável (quando o transportador aceita propostas). Null = valor de tabela. */
    @Column(name = "proposed_fee", precision = 10, scale = 2)
    private BigDecimal proposedFee;

    /** Prazo para o transportador responder; após isso, expira automaticamente. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Quando o responsável dispensou o aviso de recusa na home (null = ainda visível). */
    @Column(name = "guardian_dismissed_at")
    private Instant guardianDismissedAt;

    /** Janela padrão (em dias) para o transportador aceitar/recusar a solicitação. */
    public static final int TTL_DAYS = 3;

    /** Prazo efetivo: usa expiresAt; se nulo (registros antigos), assume created + TTL. */
    @Transient
    public Instant effectiveExpiry() {
        return expiresAt != null ? expiresAt : getCreatedAt().plus(TTL_DAYS, ChronoUnit.DAYS);
    }

    /** Solicitação pendente cujo prazo já passou. */
    @Transient
    public boolean isOverdue() {
        return status == HireStatus.PENDING && effectiveExpiry().isBefore(Instant.now());
    }
}
