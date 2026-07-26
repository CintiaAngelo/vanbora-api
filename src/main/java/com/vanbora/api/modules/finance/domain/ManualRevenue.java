package com.vanbora.api.modules.finance.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Receita lançada manualmente pelo transportador para um mês (backfill de meses
 * anteriores à entrada no app). Um registro por mês (yyyy-MM) por transportador.
 */
@Entity
@Table(name = "manual_revenues")
@Getter
@Setter
@NoArgsConstructor
public class ManualRevenue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    /** Mês de referência no formato yyyy-MM. */
    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
}
