package com.vanbora.api.modules.user.domain;

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
 * Progresso do usuário no guia de funcionalidades (onboarding). Uma linha só é criada
 * quando ele de fato marca alguma versão do guia como vista — ausência de linha
 * equivale a "nunca viu nada" (versão 0), sem precisar de backfill para contas antigas.
 */
@Entity
@Table(name = "onboarding_progress")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingProgress extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Maior versão do guia (por perfil) que o usuário já viu ou dispensou. */
    @Column(name = "last_seen_version", nullable = false)
    private int lastSeenVersion;
}
