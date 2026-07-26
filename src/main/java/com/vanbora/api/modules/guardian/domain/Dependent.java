package com.vanbora.api.modules.guardian.domain;

import com.vanbora.api.modules.school.domain.School;
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

    /** Escola do catálogo (coordenada precisa). Nullable: dados antigos só têm o nome. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School schoolRef;

    /** Foto do dependente (nullable). */
    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    /** Excluído pelo responsável (soft-delete). NULLABLE p/ ddl-auto: null ⇒ ativo. */
    @Column
    private Boolean archived;

    public boolean isArchived() {
        return Boolean.TRUE.equals(archived);
    }
}
