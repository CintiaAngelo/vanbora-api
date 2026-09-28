package com.vanbora.api.modules.trip.domain;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.ChecklistStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Checklist de segurança pós-percurso: a conferência de que nenhuma criança ficou na van.
 *
 * <p>Um por execução ({@code run_id} único — nunca há dois checklists do mesmo trajeto). A
 * conclusão é feita por UM profissional (motorista ou monitor) via UPDATE condicional
 * ({@code status = PENDING}), então dois envios simultâneos resultam numa única conclusão.
 *
 * <p>É registro de auditoria permanente: quem concluiu, com que perfil e quando ficam para
 * sempre; só os arquivos das fotos expiram (ver {@code photosPurgeAt}).
 */
@Entity
@Table(name = "safety_checklists")
@Getter
@Setter
@NoArgsConstructor
public class SafetyChecklist extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, unique = true)
    private TripRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistStatus status = ChecklistStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private User completedBy;

    /** Nome de quem concluiu, congelado no momento (o histórico não muda se a conta mudar). */
    @Column(name = "completed_by_name", length = 120)
    private String completedByName;

    /** Perfil de quem concluiu: "Motorista" ou "Monitor". */
    @Column(name = "completed_by_role", length = 20)
    private String completedByRole;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Quando as fotos devem ser excluídas (conclusão + retenção). */
    @Column(name = "photos_purge_at")
    private Instant photosPurgeAt;

    /** Quando as fotos foram de fato excluídas pela rotina de retenção. */
    @Column(name = "photos_purged_at")
    private Instant photosPurgedAt;

    /** Quantos responsáveis foram notificados da conclusão (auditoria). */
    @Column(name = "guardians_notified")
    private Integer guardiansNotified;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "safety_checklist_items", joinColumns = @JoinColumn(name = "checklist_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "item", length = 40)
    private Set<ChecklistItem> confirmedItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SafetyChecklistPhoto> photos = new ArrayList<>();
}
