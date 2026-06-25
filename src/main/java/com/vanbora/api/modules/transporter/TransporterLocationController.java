package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.transporter.dto.TransporterLocationResponse;
import com.vanbora.api.modules.transporter.dto.UpdateLocationRequest;
import com.vanbora.api.modules.transporter.service.TransporterLocationService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recebe a posição GPS do app do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me/location")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransporterLocationController {

    private final TransporterLocationService locationService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterLocationController(TransporterLocationService locationService,
                                         CurrentUserProvider currentUserProvider) {
        this.locationService = locationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public TransporterLocationResponse update(@Valid @RequestBody UpdateLocationRequest request) {
        return locationService.updateLocation(currentUserProvider.requireUserId(), request);
    }
}
