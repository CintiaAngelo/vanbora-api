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
    private Integer capacity;

    @Column(name = "years_experience")
    private Integer yearsExperience = 0;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;

    @Column(name = "base_monthly_fee", precision = 10, scale = 2)
    private BigDecimal baseMonthlyFee = BigDecimal.ZERO;

    @Column(name = "available_seats")
    private Integer availableSeats = 0;

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
