package com.vanbora.api.modules.route.repository;

import com.vanbora.api.modules.route.domain.RouteStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByTransporterIdOrderByPositionAsc(Long transporterId);
}
