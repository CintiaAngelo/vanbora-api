package com.vanbora.api.modules.route.domain;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.RouteStopStatus;
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

/** Parada da rota do dia de um transportador. */
@Entity
@Table(name = "route_stops")
@Getter
@Setter
@NoArgsConstructor
public class RouteStop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    /** Dependente (aluno) desta parada de embarque; nulo na parada da escola. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id")
    private Dependent dependent;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStopStatus status;

    @Column(name = "position", nullable = false)
    private Integer position;

    /** Coordenadas do ponto plotado no mapa (casa do aluno ou escola). */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
