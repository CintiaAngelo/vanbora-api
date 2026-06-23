package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import com.vanbora.api.modules.transporter.service.TransporterService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de busca, perfil público e perfil próprio do transportador. */
@RestController
@RequestMapping("/api/transporters")
public class TransporterController {

    private final TransporterService transporterService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterController(TransporterService transporterService,
                                 CurrentUserProvider currentUserProvider) {
        this.transporterService = transporterService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Busca por escola/bairro, ordenável por preço (padrão) ou avaliação. */
    @GetMapping
    public List<TransporterSummaryResponse> search(
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false, defaultValue = "price") String sort) {
        return transporterService.search(school, neighborhood, sort);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse myProfile() {
        return transporterService.getMyProfile(currentUserProvider.requireUserId());
    }

    @GetMapping("/{id}")
    public TransporterDetailResponse publicProfile(@PathVariable Long id) {
        return transporterService.getPublicProfile(id);
    }
}
