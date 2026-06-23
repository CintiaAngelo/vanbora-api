package com.vanbora.api.modules.route.domain;

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

    @Column(name = "label", nullable = false)
    private String label;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStopStatus status;

    @Column(name = "position", nullable = false)
    private Integer position;
}
