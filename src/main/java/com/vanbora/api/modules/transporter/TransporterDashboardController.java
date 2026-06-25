package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.enrollment.dto.StudentResponse;
import com.vanbora.api.modules.enrollment.service.StudentService;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.service.HireService;
import com.vanbora.api.modules.transporter.dto.CreateHelperRequest;
import com.vanbora.api.modules.transporter.dto.DashboardResponse;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import com.vanbora.api.modules.transporter.dto.UpdateHelperRequest;
import com.vanbora.api.modules.transporter.service.DashboardService;
import com.vanbora.api.modules.transporter.service.TransporterService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.enums.FinanceStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Painel inicial e lista de alunos do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransporterDashboardController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final HireService hireService;
    private final TransporterService transporterService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterDashboardController(
            DashboardService dashboardService,
            StudentService studentService,
            HireService hireService,
            TransporterService transporterService,
            CurrentUserProvider currentUserProvider) {
        this.dashboardService = dashboardService;
        this.studentService = studentService;
        this.hireService = hireService;
        this.transporterService = transporterService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard(currentUserProvider.requireUserId());
    }

    @GetMapping("/students")
    public List<StudentResponse> students(@RequestParam(required = false) FinanceStatus financeStatus) {
        return studentService.listMyStudents(currentUserProvider.requireUserId(), financeStatus);
    }

    /** Solicitações de contratação pendentes, com os dados do responsável. */
    @GetMapping("/hire-requests")
    public List<HireRequestResponse> hireRequests() {
        return hireService.listPendingForTransporter(currentUserProvider.requireUserId());
    }

    @GetMapping("/helpers")
    public List<HelperResponse> helpers() {
        return transporterService.listMyHelpers(currentUserProvider.requireUserId());
    }

    @PostMapping("/helpers")
    public ResponseEntity<HelperResponse> addHelper(@Valid @RequestBody CreateHelperRequest request) {
        HelperResponse response =
                transporterService.addHelper(currentUserProvider.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/helpers/{id}")
    public HelperResponse updateHelper(@PathVariable Long id, @Valid @RequestBody UpdateHelperRequest request) {
        return transporterService.updateHelper(currentUserProvider.requireUserId(), id, request);
    }

    @PostMapping("/helpers/{id}/photo")
    public HelperResponse uploadHelperPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return transporterService.setHelperPhoto(currentUserProvider.requireUserId(), id, file);
    }

    @DeleteMapping("/helpers/{id}")
    public ResponseEntity<Void> deleteHelper(@PathVariable Long id) {
        transporterService.deleteHelper(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}

