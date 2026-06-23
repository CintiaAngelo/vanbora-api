package com.vanbora.api.modules.enrollment.domain;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.FinanceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vínculo ativo entre um dependente e um transportador (a "matrícula").
 * Representa simultaneamente o aluno transportado e o contrato do responsável.
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_id", nullable = false)
    private Dependent dependent;

    @Column(name = "monthly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal monthlyFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "finance_status", nullable = false, length = 20)
    private FinanceStatus financeStatus = FinanceStatus.EM_DIA;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendance = new ArrayList<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.vanbora.api.modules.payment.domain.Payment> payments = new ArrayList<>();
}
