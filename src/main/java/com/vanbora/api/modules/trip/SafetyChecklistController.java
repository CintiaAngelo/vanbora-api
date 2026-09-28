package com.vanbora.api.modules.trip;

import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse;
import com.vanbora.api.modules.trip.service.SafetyChecklistService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.enums.ChecklistStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Checklist de segurança pós-percurso — o mesmo para motorista e monitor. */
@RestController
@RequestMapping("/api/safety-checklists")
@PreAuthorize("hasAnyRole('TRANSPORTER', 'MONITOR')")
public class SafetyChecklistController {

    private final SafetyChecklistService checklistService;
    private final CurrentUserProvider currentUserProvider;

    public SafetyChecklistController(SafetyChecklistService checklistService,
                                     CurrentUserProvider currentUserProvider) {
        this.checklistService = checklistService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<SafetyChecklistResponse> list(@RequestParam(required = false) ChecklistStatus status) {
        return checklistService.list(currentUserProvider.requireUser(), status);
    }

    @GetMapping("/{id}")
    public SafetyChecklistResponse get(@PathVariable Long id) {
        return checklistService.get(currentUserProvider.requireUser(), id);
    }

    /**
     * Conclui o checklist (multipart): {@code items} = códigos confirmados (todos obrigatórios)
     * e {@code photos} = fotos do interior da van (ao menos uma). 409 se outro profissional já
     * concluiu — nesse caso nada deste envio é gravado.
     */
    @PostMapping("/{id}/complete")
    public SafetyChecklistResponse complete(@PathVariable Long id,
                                            @RequestParam(value = "items", required = false) List<String> items,
                                            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        return checklistService.complete(currentUserProvider.requireUser(), id, items, photos);
    }
}
