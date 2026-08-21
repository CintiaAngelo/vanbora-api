package com.vanbora.api.modules.schoolportal.service;

import com.vanbora.api.modules.schoolportal.dto.ActivityItemResponse;
import com.vanbora.api.modules.schoolportal.dto.MonthlyTrendPointResponse;
import com.vanbora.api.modules.schoolportal.dto.SchoolProfileResponse;
import com.vanbora.api.modules.schoolportal.dto.StudentDetailResponse;
import com.vanbora.api.modules.schoolportal.dto.TransporterStatsResponse;
import java.util.List;

/** Casos de uso do painel de gestão da escola autenticada (perfil SCHOOL). */
public interface SchoolPortalService {

    SchoolProfileResponse getProfile();

    List<TransporterStatsResponse> getTransporters();

    List<StudentDetailResponse> getStudents();

    List<ActivityItemResponse> getRecentActivity();

    List<MonthlyTrendPointResponse> getMonthlyTrend();
}
