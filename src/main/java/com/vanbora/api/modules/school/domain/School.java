package com.vanbora.api.modules.school.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Escola do catálogo global. Cadastrada pelos transportadores (por CEP) e
 * escolhida pelos responsáveis. Geocodificada pelo endereço — coordenada precisa.
 */
@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
public class School extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 9)
    private String cep;

    private String street;
    private String number;
    private String neighborhood;
    private String city;

    @Column(length = 2)
    private String uf;

    private Double latitude;
    private Double longitude;
}
