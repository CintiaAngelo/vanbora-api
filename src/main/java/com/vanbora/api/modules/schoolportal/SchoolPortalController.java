package com.vanbora.api.modules.schoolportal;

import com.vanbora.api.modules.schoolportal.dto.ActivityItemResponse;
import com.vanbora.api.modules.schoolportal.dto.MonthlyTrendPointResponse;
import com.vanbora.api.modules.schoolportal.dto.SchoolProfileResponse;
import com.vanbora.api.modules.schoolportal.dto.StudentDetailResponse;
import com.vanbora.api.modules.schoolportal.dto.TransporterStatsResponse;
import com.vanbora.api.modules.schoolportal.service.SchoolPortalService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Painel de gestão da escola: dados restritos aos transportadores e alunos vinculados à escola
 * da conta autenticada (perfil SCHOOL). Consumido pelo vanbora-school-panel através do
 * vanbora-bff-escolas.
 */
@RestController
@RequestMapping("/api/schools/me")
@PreAuthorize("hasRole('SCHOOL')")
public class SchoolPortalController {

    private final SchoolPortalService service;

    public SchoolPortalController(SchoolPortalService service) {
        this.service = service;
    }

    @GetMapping
    public SchoolProfileResponse profile() {
        return service.getProfile();
    }

    @GetMapping("/transporters")
    public List<TransporterStatsResponse> transporters() {
        return service.getTransporters();
    }

    @GetMapping("/students")
    public List<StudentDetailResponse> students() {
        return service.getStudents();
    }

    @GetMapping("/activity")
    public List<ActivityItemResponse> recentActivity() {
        return service.getRecentActivity();
    }

    @GetMapping("/monthly-trend")
    public List<MonthlyTrendPointResponse> monthlyTrend() {
        return service.getMonthlyTrend();
    }
}
