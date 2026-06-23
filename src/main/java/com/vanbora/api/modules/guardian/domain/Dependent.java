package com.vanbora.api.modules.guardian.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dependente (aluno) vinculado a um responsável. */
@Entity
@Table(name = "dependents")
@Getter
@Setter
@NoArgsConstructor
public class Dependent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String school;
}
