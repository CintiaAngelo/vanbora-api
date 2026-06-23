package com.vanbora.api.modules.notice.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
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

/** Aviso em massa enviado por um transportador aos responsáveis vinculados. */
@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
public class Notice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "recipients_count", nullable = false)
    private Integer recipientsCount;
}
