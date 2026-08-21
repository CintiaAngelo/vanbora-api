package com.vanbora.api.modules.enrollment.repository;

import com.vanbora.api.modules.enrollment.domain.Absence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    Optional<Absence> findByEnrollmentIdAndDate(Long enrollmentId, LocalDate date);

    boolean existsByEnrollmentIdAndDate(Long enrollmentId, LocalDate date);

    /**
     * Ids dos dependentes com falta avisada numa data, entre as matrículas ATIVAS de um
     * transportador. Usado para excluir quem não vai hoje da ordem de embarque da rota
     * (transportador e rastreamento do responsável) sem persistir nada em RouteStop —
     * a falta é a única fonte da verdade, e "hoje" é sempre calculado na leitura.
     */
    @Query("select a.enrollment.dependent.id from Absence a "
            + "where a.date = :date and a.enrollment.transporter.id = :transporterId and a.enrollment.active = true")
    Set<Long> findAbsentDependentIds(@Param("transporterId") Long transporterId, @Param("date") LocalDate date);

    List<Absence> findByEnrollmentIdAndDateBetween(Long enrollmentId, LocalDate from, LocalDate to);

    /** Faltas avisadas numa data específica, para um conjunto de matrículas (visão do painel escolar). */
    List<Absence> findByEnrollmentIdInAndDate(List<Long> enrollmentIds, LocalDate date);

    /** Faltas avisadas recentemente (atividade recente do painel escolar). */
    List<Absence> findByEnrollmentIdInAndDateBetweenOrderByCreatedAtDesc(
            List<Long> enrollmentIds, LocalDate from, LocalDate to);
}
