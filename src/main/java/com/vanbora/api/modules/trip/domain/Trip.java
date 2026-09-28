package com.vanbora.api.modules.trip.domain;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Percurso do transportador (ex.: "Ida — manhã"): quais monitores trabalham nele e quais
 * alunos ele leva. É a base de todas as permissões do monitor — ele só enxerga alunos,
 * checklists e conversas ligados aos percursos em que está escalado.
 *
 * O público segue o mesmo padrão dos avisos: {@code allStudents} (todos os alunos ativos
 * do transportador, inclusive os que forem contratados depois) ou a lista explícita.
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
public class Trip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private TransporterProfile transporter;

    @Column(nullable = false, length = 80)
    private String name;

    /** Excluir um percurso só o desativa: execuções e checklists antigos continuam no histórico. */
    private Boolean active = true;

    /** Leva todos os alunos ativos. NULLABLE: null ⇒ todos. */
    @Column(name = "all_students")
    private Boolean allStudents = true;

    /** Monitores escalados (ajudantes com conta de acesso). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "trip_monitors",
            joinColumns = @JoinColumn(name = "trip_id"),
            inverseJoinColumns = @JoinColumn(name = "helper_id"))
    private Set<Helper> monitors = new LinkedHashSet<>();

    /** Alunos do percurso quando {@code allStudents} é falso. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "trip_students",
            joinColumns = @JoinColumn(name = "trip_id"),
            inverseJoinColumns = @JoinColumn(name = "dependent_id"))
    private Set<Dependent> students = new LinkedHashSet<>();

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isAllStudents() {
        return allStudents == null || allStudents;
    }

    /** O dependente é transportado neste percurso? (considera "todos os alunos"). */
    public boolean includesDependent(Long dependentId) {
        return isAllStudents() || students.stream().anyMatch(d -> d.getId().equals(dependentId));
    }

    public boolean hasMonitor(Long helperId) {
        return monitors.stream().anyMatch(h -> h.getId().equals(helperId));
    }
}
