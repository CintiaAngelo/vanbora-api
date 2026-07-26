package com.vanbora.api.modules.hire;

import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.GuardianForTransporterResponse;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.service.HireService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de solicitação e resposta de contratação. */
@RestController
@RequestMapping("/api/hire-requests")
public class HireController {

    private final HireService hireService;
    private final CurrentUserProvider currentUserProvider;

    public HireController(HireService hireService, CurrentUserProvider currentUserProvider) {
        this.hireService = hireService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<HireRequestResponse> create(@Valid @RequestBody CreateHireRequest request) {
        HireRequestResponse response =
                hireService.requestHire(currentUserProvider.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** [Transportador] Perfil do responsável de uma solicitação recebida. */
    @GetMapping("/{id}/guardian")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public GuardianForTransporterResponse guardian(@PathVariable Long id) {
        return hireService.getGuardianForRequest(currentUserProvider.requireUserId(), id);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        hireService.accept(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        hireService.reject(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /** [Responsável] Cancela a própria solicitação pendente. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        hireService.cancelHireRequest(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /** [Responsável] Dispensa o aviso de uma solicitação recusada. */
    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        hireService.dismissHireRequest(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
