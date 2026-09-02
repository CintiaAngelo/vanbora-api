package com.vanbora.api.modules.user.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identidade de autenticação, comum aos dois perfis. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hash BCrypt da senha. Contas criadas por login social recebem aqui o hash de
     * um valor aleatório descartado — nenhuma senha digitada casa com ele, e o
     * campo continua obrigatório (não exige migração de schema). Para definir uma
     * senha utilizável, essas contas usam a recuperação de senha.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** Provedor do login social vinculado: {@code google}, {@code facebook} ou nulo. */
    @Column(name = "auth_provider", length = 40)
    private String authProvider;

    /** Identificador da conta no provedor ({@code sub} do Auth0), quando houver. */
    @Column(name = "auth_subject", length = 120)
    private String authSubject;
}
