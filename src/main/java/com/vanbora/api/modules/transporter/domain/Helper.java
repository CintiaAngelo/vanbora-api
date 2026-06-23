package com.vanbora.api.modules.transporter.domain;

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

/** Ajudante/monitor vinculado a um transportador. */
@Entity
@Table(name = "helpers")
@Getter
@Setter
@NoArgsConstructor
public class Helper extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;
}
