package com.vanbora.api.modules.trip.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.TripRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma execução do percurso num dia (iniciada → encerrada ou cancelada). Ao ser
 * encerrada gera exatamente um {@link SafetyChecklist}; cancelada, não gera nenhum.
 */
@Entity
@Table(name = "trip_runs")
@Getter
@Setter
@NoArgsConstructor
public class TripRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /** Denormalizado para isolar consultas por transportador sem passar pelo percurso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_id")
    private User startedBy;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finished_by_id")
    private User finishedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Retrato (auditoria) dos monitores escalados no momento em que o percurso começou.
     * O ACESSO usa a escala atual do percurso; isto só registra quem participou.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "trip_run_monitors",
            joinColumns = @JoinColumn(name = "run_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> monitorsOnDuty = new LinkedHashSet<>();
}
