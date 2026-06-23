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
}
