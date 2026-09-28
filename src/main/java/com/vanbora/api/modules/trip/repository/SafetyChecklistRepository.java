package com.vanbora.api.modules.trip.repository;

import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.ChecklistStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyChecklistRepository extends JpaRepository<SafetyChecklist, Long> {

    Optional<SafetyChecklist> findByRunId(Long runId);

    /** Checklists do transportador (todos os percursos), mais recentes primeiro. */
    @Query("""
            select c from SafetyChecklist c
            where c.transporter.id = :transporterId
              and (:status is null or c.status = :status)
            order by c.run.finishedAt desc
            """)
    List<SafetyChecklist> findForTransporter(@Param("transporterId") Long transporterId,
                                             @Param("status") ChecklistStatus status);

    /** Checklists dos percursos informados (os do monitor), mais recentes primeiro. */
    @Query("""
            select c from SafetyChecklist c
            where c.run.trip.id in :tripIds
              and (:status is null or c.status = :status)
            order by c.run.finishedAt desc
            """)
    List<SafetyChecklist> findForTrips(@Param("tripIds") Collection<Long> tripIds,
                                       @Param("status") ChecklistStatus status);

    /** Conclusões recentes dos percursos informados (visão do responsável). */
    @Query("""
            select c from SafetyChecklist c
            where c.run.trip.id in :tripIds
              and c.status = com.vanbora.api.shared.enums.ChecklistStatus.COMPLETED
              and c.completedAt >= :since
            order by c.completedAt desc
            """)
    List<SafetyChecklist> findCompletedForTripsSince(@Param("tripIds") Collection<Long> tripIds,
                                                     @Param("since") Instant since);

    /**
     * Conclusão ATÔMICA: só altera o checklist se ele ainda estiver pendente. No MySQL
     * (InnoDB) o segundo UPDATE simultâneo espera o lock da linha, reavalia o WHERE com o
     * valor já confirmado e afeta 0 linhas — há uma única conclusão válida por percurso.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SafetyChecklist c
               set c.status = com.vanbora.api.shared.enums.ChecklistStatus.COMPLETED,
                   c.completedBy = :user, c.completedByName = :name, c.completedByRole = :role,
                   c.completedAt = :now, c.photosPurgeAt = :purgeAt
             where c.id = :id and c.status = com.vanbora.api.shared.enums.ChecklistStatus.PENDING
            """)
    int completeIfPending(@Param("id") Long id,
                          @Param("user") User user,
                          @Param("name") String name,
                          @Param("role") String role,
                          @Param("now") Instant now,
                          @Param("purgeAt") Instant purgeAt);
}
