package com.vanbora.api.modules.school.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

    /**
     * Conta de acesso ao painel de gestão da escola (perfil SCHOOL). Nullable: a maioria das
     * escolas do catálogo é só um registro de endereço, cadastrado por um transportador, sem
     * login próprio — só passa a ter uma conta quando a escola parceira adere ao painel.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
