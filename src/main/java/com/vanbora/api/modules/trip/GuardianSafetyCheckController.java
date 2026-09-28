package com.vanbora.api.modules.trip;

import com.vanbora.api.modules.trip.dto.GuardianSafetyCheckResponse;
import com.vanbora.api.modules.trip.service.SafetyChecklistService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** [Responsável] Conferências de segurança recentes nos percursos do filho. */
@RestController
@RequestMapping("/api/guardians/me/safety-checks")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianSafetyCheckController {

    private final SafetyChecklistService checklistService;
    private final CurrentUserProvider currentUserProvider;

    public GuardianSafetyCheckController(SafetyChecklistService checklistService,
                                         CurrentUserProvider currentUserProvider) {
        this.checklistService = checklistService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<GuardianSafetyCheckResponse> recent(@RequestParam(required = false) Long dependentId) {
        return checklistService.recentForGuardian(currentUserProvider.requireUser(), dependentId);
    }
}
