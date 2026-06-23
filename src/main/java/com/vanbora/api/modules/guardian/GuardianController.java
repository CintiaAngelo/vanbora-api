package com.vanbora.api.modules.guardian;

import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.guardian.service.GuardianService;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do responsável autenticado. */
@RestController
@RequestMapping("/api/guardians/me")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianController {

    private final GuardianService guardianService;
    private final CurrentUserProvider currentUserProvider;

    public GuardianController(GuardianService guardianService, CurrentUserProvider currentUserProvider) {
        this.guardianService = guardianService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public GuardianProfileResponse profile() {
        return guardianService.getMyProfile(userId());
    }

    @GetMapping("/dashboard")
    public GuardianDashboardResponse dashboard() {
        return guardianService.getDashboard(userId());
    }

    @GetMapping("/dependents")
    public List<DependentResponse> dependents() {
        return guardianService.listDependents(userId());
    }

    @PostMapping("/dependents")
    public ResponseEntity<DependentResponse> addDependent(@Valid @RequestBody CreateDependentRequest request) {
        DependentResponse response = guardianService.addDependent(userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments() {
        return guardianService.listPayments(userId());
    }

    private Long userId() {
        return currentUserProvider.requireUserId();
    }
}
