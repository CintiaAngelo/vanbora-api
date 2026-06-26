package com.vanbora.api.modules.enrollment.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Falta avisada pelo responsável: o aluno é presente por padrão; cada registro
 * marca um DIA específico em que ele NÃO vai. Ausência de registro = vai.
 */
@Entity
@Table(
        name = "absences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "absence_date"}))
@Getter
@Setter
@NoArgsConstructor
public class Absence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "absence_date", nullable = false)
    private LocalDate date;
}
