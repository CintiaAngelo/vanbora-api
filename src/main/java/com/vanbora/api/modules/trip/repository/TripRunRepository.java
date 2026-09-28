package com.vanbora.api.modules.trip.repository;

import com.vanbora.api.modules.trip.domain.TripRun;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.TripRunStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRunRepository extends JpaRepository<TripRun, Long> {

    Optional<TripRun> findFirstByTripIdAndStatus(Long tripId, TripRunStatus status);

    /** Execução mais recente do percurso (estado exibido no cartão do percurso). */
    Optional<TripRun> findFirstByTripIdOrderByStartedAtDesc(Long tripId);

    /**
     * Transição atômica EM ANDAMENTO → ENCERRADO. Devolve 1 só para o primeiro pedido; um
     * segundo "encerrar" simultâneo encontra o status já alterado e recebe 0 — evitando
     * dois checklists para o mesmo trajeto (reforçado pelo run_id único do checklist).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TripRun r set r.status = com.vanbora.api.shared.enums.TripRunStatus.COMPLETED,
                   r.finishedAt = :now, r.finishedBy = :user
            where r.id = :id and r.status = com.vanbora.api.shared.enums.TripRunStatus.IN_PROGRESS
            """)
    int finishIfInProgress(@Param("id") Long id, @Param("user") User user, @Param("now") Instant now);

    /** Transição atômica EM ANDAMENTO → CANCELADO (percurso que não ocorreu: sem checklist). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TripRun r set r.status = com.vanbora.api.shared.enums.TripRunStatus.CANCELLED,
                   r.cancelledAt = :now
            where r.id = :id and r.status = com.vanbora.api.shared.enums.TripRunStatus.IN_PROGRESS
            """)
    int cancelIfInProgress(@Param("id") Long id, @Param("now") Instant now);
}
