package com.vanbora.api.modules.trip;

import com.vanbora.api.modules.trip.dto.TripResponse;
import com.vanbora.api.modules.trip.dto.TripRunResponse;
import com.vanbora.api.modules.trip.service.TripService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Percursos e execução (iniciar/encerrar/cancelar), para o transportador e os monitores. O
 * escopo de cada um (todos os percursos ou só os escalados) é decidido no serviço.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('TRANSPORTER', 'MONITOR')")
public class TripController {

    private final TripService tripService;
    private final CurrentUserProvider currentUserProvider;

    public TripController(TripService tripService, CurrentUserProvider currentUserProvider) {
        this.tripService = tripService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/trips")
    public List<TripResponse> trips() {
        return tripService.list(currentUserProvider.requireUser());
    }

    @PostMapping("/trips/{id}/runs")
    public ResponseEntity<TripRunResponse> start(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.start(currentUserProvider.requireUser(), id));
    }

    @PostMapping("/trip-runs/{id}/finish")
    public TripRunResponse finish(@PathVariable Long id) {
        return tripService.finish(currentUserProvider.requireUser(), id);
    }

    @PostMapping("/trip-runs/{id}/cancel")
    public TripRunResponse cancel(@PathVariable Long id) {
        return tripService.cancel(currentUserProvider.requireUser(), id);
    }
}
