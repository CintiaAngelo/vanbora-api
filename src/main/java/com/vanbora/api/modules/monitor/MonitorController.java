package com.vanbora.api.modules.monitor;

import com.vanbora.api.modules.monitor.dto.MonitorProfileResponse;
import com.vanbora.api.modules.monitor.dto.MonitorStudentResponse;
import com.vanbora.api.modules.monitor.service.MonitorService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Área do monitor autenticado. Não recebe ids do cliente: tudo é derivado da própria conta,
 * então não há como pedir dados de outro monitor/transportador trocando um id na URL.
 */
@RestController
@RequestMapping("/api/monitors/me")
@PreAuthorize("hasRole('MONITOR')")
public class MonitorController {

    private final MonitorService monitorService;
    private final CurrentUserProvider currentUserProvider;

    public MonitorController(MonitorService monitorService, CurrentUserProvider currentUserProvider) {
        this.monitorService = monitorService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public MonitorProfileResponse me() {
        return monitorService.profile(currentUserProvider.requireUser());
    }

    @GetMapping("/students")
    public List<MonitorStudentResponse> students() {
        return monitorService.students(currentUserProvider.requireUser());
    }
}
