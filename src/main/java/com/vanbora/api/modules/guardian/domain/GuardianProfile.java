package com.vanbora.api.modules.guardian.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dados específicos do perfil Responsável. */
@Entity
@Table(name = "guardian_profiles")
@Getter
@Setter
@NoArgsConstructor
public class GuardianProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String cpf;

    // ----- Endereço de embarque (buscar a criança) -----
    private String cep;
    private String city;
    private String neighborhood;
    private String street;
    private String number;

    /** Coordenadas do embarque (geocodificadas) para o mapa/rota. */
    private Double latitude;
    private Double longitude;

    // ----- Endereço de entrega (deixar a criança) -----
    @Column(name = "delivery_cep")
    private String deliveryCep;

    @Column(name = "delivery_city")
    private String deliveryCity;

    @Column(name = "delivery_neighborhood")
    private String deliveryNeighborhood;

    @Column(name = "delivery_street")
    private String deliveryStreet;

    @Column(name = "delivery_number")
    private String deliveryNumber;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @OneToMany(mappedBy = "guardian", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependent> dependents = new ArrayList<>();

    public void addDependent(Dependent dependent) {
        dependent.setGuardian(this);
        this.dependents.add(dependent);
    }
}
