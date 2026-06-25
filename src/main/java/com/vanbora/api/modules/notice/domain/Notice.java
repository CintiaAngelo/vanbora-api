package com.vanbora.api.modules.notice.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.NoticePriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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

    /** Título opcional, exibido em destaque. */
    @Column(name = "title", length = 120)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "recipients_count", nullable = false)
    private Integer recipientsCount;

    /**
     * A partir de quando o aviso fica visível aos responsáveis (agendamento).
     * Coluna nullable para o ddl-auto=update conseguir adicioná-la a tabelas já
     * populadas; na prática é sempre preenchida (na criação e no backfill).
     */
    @Column(name = "publish_at")
    private Instant publishAt;

    /** Se aceita comentários dos responsáveis. Nullable (ddl-auto); null⇒true. */
    @Column(name = "allow_comments")
    private Boolean allowComments;

    /** Prioridade. Nullable (ddl-auto); null⇒INFO. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 16)
    private NoticePriority priority;

    /** Se o aviso vai para todos os responsáveis. Nullable (ddl-auto); null⇒true. */
    @Column(name = "audience_all")
    private Boolean audienceAll;

    public boolean isAllowComments() {
        return allowComments == null || allowComments;
    }

    public boolean isAudienceAll() {
        return audienceAll == null || audienceAll;
    }

    public NoticePriority resolvedPriority() {
        return priority == null ? NoticePriority.INFO : priority;
    }
}
