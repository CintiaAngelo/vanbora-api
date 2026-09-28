package com.vanbora.api.modules.trip;

import com.vanbora.api.modules.trip.dto.TripRequest;
import com.vanbora.api.modules.trip.dto.TripResponse;
import com.vanbora.api.modules.trip.service.TripService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [Transportador] Cadastro dos percursos: nome, monitores escalados e alunos. */
@RestController
@RequestMapping("/api/transporters/me/trips")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransporterTripController {

    private final TripService tripService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterTripController(TripService tripService, CurrentUserProvider currentUserProvider) {
        this.tripService = tripService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.create(currentUserProvider.requireUser(), request));
    }

    @PutMapping("/{id}")
    public TripResponse update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
        return tripService.update(currentUserProvider.requireUser(), id, request);
    }

    /** Desativa o percurso; o histórico de execuções e checklists é preservado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripService.delete(currentUserProvider.requireUser(), id);
        return ResponseEntity.noContent().build();
    }
}
