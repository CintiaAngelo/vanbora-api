package com.vanbora.api.modules.hire.domain;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrato de transporte liberado pelo transportador e assinado pelo responsável.
 * Nasce {@code PENDING_SIGNATURE} ao liberar e vira {@code ACTIVE} ao assinar,
 * quando a matrícula é criada e a primeira mensalidade é cobrada.
 */
@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class Contract extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hire_request_id", nullable = false)
    private HireRequest hireRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_id", nullable = false)
    private Dependent dependent;

    @Column(name = "monthly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal monthlyFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status = ContractStatus.PENDING_SIGNATURE;

    /** Token do link de assinatura enviado por e-mail. */
    @Column(name = "signature_token", nullable = false, unique = true)
    private String signatureToken;

    @Column(name = "signed_at")
    private Instant signedAt;

    /** Matrícula criada quando o contrato é assinado (nula enquanto pendente). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;
}
