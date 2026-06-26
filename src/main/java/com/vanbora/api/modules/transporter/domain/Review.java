package com.vanbora.api.modules.transporter.domain;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
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

/** Avaliação de um responsável sobre um transportador. */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    /** Responsável autor (nullable: avaliações de demonstração não têm vínculo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private GuardianProfile guardian;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String comment;
}
