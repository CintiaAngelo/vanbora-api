package com.vanbora.api.modules.notice.domain;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Destinatário específico de um aviso segmentado. Só existe quando o aviso NÃO é
 * para todos ({@code audienceAll=false}).
 */
@Entity
@Table(name = "notice_recipients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notice_id", "guardian_id"}))
@Getter
@Setter
@NoArgsConstructor
public class NoticeRecipient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;
}
