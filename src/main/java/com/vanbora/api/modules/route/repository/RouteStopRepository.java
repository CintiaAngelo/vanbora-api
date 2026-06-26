package com.vanbora.api.modules.route.repository;

import com.vanbora.api.modules.route.domain.RouteStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByTransporterIdOrderByPositionAsc(Long transporterId);

    /** Remove a parada de embarque de um aluno (ao cancelar a matrícula). */
    @Transactional
    void deleteByTransporterIdAndDependentId(Long transporterId, Long dependentId);
}
