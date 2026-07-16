package com.vanbora.api.modules.transporter.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Janela agendada em que o transportador compartilha sua localização automaticamente
 * (ex.: segunda, 06:00–07:00). Uma janela cobre um dia da semana e um intervalo.
 */
@Entity
@Table(name = "location_share_windows")
@Getter
@Setter
@NoArgsConstructor
public class LocationShareWindow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    /** Dia da semana no padrão ISO: 1 = segunda … 7 = domingo. */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
