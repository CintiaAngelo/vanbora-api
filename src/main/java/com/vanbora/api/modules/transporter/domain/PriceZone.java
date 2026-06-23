package com.vanbora.api.modules.transporter.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Zona de precificação por escola (ex.: Premium, Standard). */
@Entity
@Table(name = "price_zones")
@Getter
@Setter
@NoArgsConstructor
public class PriceZone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(nullable = false)
    private String name;

    private String school;

    @Column(name = "monthly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal monthlyFee;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "price_zone_neighborhoods", joinColumns = @JoinColumn(name = "price_zone_id"))
    @Column(name = "neighborhood")
    private Set<String> neighborhoods = new LinkedHashSet<>();
}
