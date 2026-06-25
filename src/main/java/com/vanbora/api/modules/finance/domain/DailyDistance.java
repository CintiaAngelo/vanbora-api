package com.vanbora.api.modules.finance.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quilometragem rodada por um transportador em um dia (derivada do GPS). */
@Entity
@Table(name = "daily_distances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"transporter_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyDistance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "km", nullable = false)
    private double km;
}
