package com.vanbora.api.modules.enrollment.domain;

import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Presença do aluno em um dia da semana. */
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private boolean present;
}
