package com.vanbora.api.modules.trip.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto do interior da van anexada ao checklist. O arquivo fica no armazenamento PRIVADO
 * (nunca em /uploads, que é público) e só é servido por URL assinada a quem tem acesso.
 * Após a retenção o arquivo é apagado e {@code storageKey} zerado — a linha permanece
 * como prova de que houve foto, quem enviou e quando.
 */
@Entity
@Table(name = "safety_checklist_photos")
@Getter
@Setter
@NoArgsConstructor
public class SafetyChecklistPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private SafetyChecklist checklist;

    /** Chave no armazenamento privado; nula depois que a foto expirou. */
    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "content_type", length = 40)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "purged_at")
    private Instant purgedAt;

    public boolean isAvailable() {
        return storageKey != null && purgedAt == null;
    }
}
