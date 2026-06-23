package com.vanbora.api.modules.route;

import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota do dia do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me/route")
@PreAuthorize("hasRole('TRANSPORTER')")
public class RouteController {

    private final RouteStopRepository routeStopRepository;
    private final TransporterProfileRepository transporterRepository;
    private final CurrentUserProvider currentUserProvider;

    public RouteController(RouteStopRepository routeStopRepository,
                           TransporterProfileRepository transporterRepository,
                           CurrentUserProvider currentUserProvider) {
        this.routeStopRepository = routeStopRepository;
        this.transporterRepository = transporterRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RouteStopResponse> route() {
        Long userId = currentUserProvider.requireUserId();
        Long transporterId = transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId))
                .getId();
        return routeStopRepository.findByTransporterIdOrderByPositionAsc(transporterId).stream()
                .map(RouteStopResponse::from)
                .toList();
    }
}
