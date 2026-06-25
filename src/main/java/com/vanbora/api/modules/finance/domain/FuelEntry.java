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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Abastecimento: litros + valor, base para o consumo (km/l) e custo por km. */
@Entity
@Table(name = "fuel_entries")
@Getter
@Setter
@NoArgsConstructor
public class FuelEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double liters;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
}
