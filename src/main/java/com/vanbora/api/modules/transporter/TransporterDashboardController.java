package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.enrollment.dto.StudentResponse;
import com.vanbora.api.modules.enrollment.service.StudentService;
import com.vanbora.api.modules.transporter.dto.DashboardResponse;
import com.vanbora.api.modules.transporter.service.DashboardService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.enums.FinanceStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Painel inicial e lista de alunos do transportador autenticado. */
@RestController
@RequestMapping("/api/transporters/me")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransporterDashboardController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterDashboardController(
            DashboardService dashboardService,
            StudentService studentService,
            CurrentUserProvider currentUserProvider) {
        this.dashboardService = dashboardService;
        this.studentService = studentService;
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
}
