package com.vanbora.api.modules.route;

import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.service.RouteService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota do dia do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me/route")
@PreAuthorize("hasRole('TRANSPORTER')")
public class RouteController {

    private final RouteService routeService;
    private final CurrentUserProvider currentUserProvider;

    public RouteController(RouteService routeService, CurrentUserProvider currentUserProvider) {
        this.routeService = routeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<RouteStopResponse> route() {
        return routeService.listForUser(currentUserProvider.requireUserId());
    }

    /** Recalcula a melhor ordem das paradas e devolve a rota já reordenada. */
    @PostMapping("/optimize")
    public List<RouteStopResponse> optimize() {
        return routeService.optimizeForUser(currentUserProvider.requireUserId());
    }
}
