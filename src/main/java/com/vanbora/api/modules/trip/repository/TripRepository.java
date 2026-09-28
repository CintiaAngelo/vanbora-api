package com.vanbora.api.modules.trip.repository;

import com.vanbora.api.modules.trip.domain.Trip;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

    /** Percursos ativos do transportador, em ordem de criação. */
    @Query("""
            select t from Trip t
            where t.transporter.id = :transporterId and (t.active is null or t.active = true)
            order by t.id asc
            """)
    List<Trip> findActiveByTransporter(@Param("transporterId") Long transporterId);

    /** Percursos ativos em que o ajudante (monitor) está escalado. */
    @Query("""
            select t from Trip t join t.monitors m
            where m.id = :helperId and (t.active is null or t.active = true)
            order by t.id asc
            """)
    List<Trip> findActiveByMonitor(@Param("helperId") Long helperId);

    /** Todos os percursos (inclusive inativos) em que o ajudante aparece — para desvinculá-lo. */
    @Query("select t from Trip t join t.monitors m where m.id = :helperId")
    List<Trip> findAllByMonitor(@Param("helperId") Long helperId);

    Optional<Trip> findByIdAndTransporterId(Long id, Long transporterId);

    /**
     * Trava a linha do percurso até o fim da transação: serializa dois "iniciar"
     * simultâneos, garantindo no máximo uma execução em andamento por percurso.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
