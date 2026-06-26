package com.vanbora.api.modules.transporter.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dados profissionais do perfil Transportador. */
@Entity
@Table(name = "transporter_profiles")
@Getter
@Setter
@NoArgsConstructor
public class TransporterProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String document;
    private String cnh;
    private String plate;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "years_experience")
    private Integer yearsExperience = 0;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;

    @Column(name = "base_monthly_fee", precision = 10, scale = 2)
    private BigDecimal baseMonthlyFee = BigDecimal.ZERO;

    /** Se aceita propostas de valor do responsável. NULLABLE (ddl-auto): null ⇒ não. */
    @Column(name = "accepts_proposals")
    private Boolean acceptsProposals = false;

    /** Considera nulo como "não aceita propostas". */
    public boolean isAcceptsProposals() {
        return Boolean.TRUE.equals(acceptsProposals);
    }

    /** Última posição conhecida do transportador (atualizada pelo GPS do app). */
    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    /** Configurações financeiras (nullable para o ddl-auto em tabela populada). */
    @Column(name = "monthly_revenue_goal", precision = 10, scale = 2)
    private BigDecimal monthlyRevenueGoal;

    @Column(name = "maintenance_interval_km")
    private Integer maintenanceIntervalKm;

    /** Km acumulado registrado na última manutenção (para calcular o quanto falta). */
    @Column(name = "last_maintenance_km")
    private Double lastMaintenanceKm;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "transporter_schools", joinColumns = @JoinColumn(name = "transporter_id"))
    @Column(name = "school")
    private Set<String> schools = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "transporter_neighborhoods", joinColumns = @JoinColumn(name = "transporter_id"))
    @Column(name = "neighborhood")
    private Set<String> neighborhoods = new LinkedHashSet<>();

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceZone> priceZones = new ArrayList<>();

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Helper> helpers = new ArrayList<>();

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
}
