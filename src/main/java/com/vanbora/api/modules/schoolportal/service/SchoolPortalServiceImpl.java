package com.vanbora.api.modules.schoolportal.service;

import com.vanbora.api.modules.enrollment.domain.Absence;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.modules.schoolportal.dto.ActivityItemResponse;
import com.vanbora.api.modules.schoolportal.dto.MonthlyTrendPointResponse;
import com.vanbora.api.modules.schoolportal.dto.SchoolProfileResponse;
import com.vanbora.api.modules.schoolportal.dto.StudentDetailResponse;
import com.vanbora.api.modules.schoolportal.dto.TransporterStatsResponse;
import com.vanbora.api.modules.transporter.domain.Review;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.ReviewRepository;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação: todas as consultas são restritas às matrículas ativas da escola autenticada. */
@Service
@Transactional(readOnly = true)
public class SchoolPortalServiceImpl implements SchoolPortalService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final SchoolRepository schoolRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AbsenceRepository absenceRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserProvider currentUserProvider;

    public SchoolPortalServiceImpl(
            SchoolRepository schoolRepository,
            EnrollmentRepository enrollmentRepository,
            AbsenceRepository absenceRepository,
            ReviewRepository reviewRepository,
            CurrentUserProvider currentUserProvider) {
        this.schoolRepository = schoolRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.absenceRepository = absenceRepository;
        this.reviewRepository = reviewRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public SchoolProfileResponse getProfile() {
        return SchoolProfileResponse.from(currentSchool());
    }

    @Override
    public List<TransporterStatsResponse> getTransporters() {
        School school = currentSchool();
        List<Enrollment> enrollments = enrollmentRepository.findByDependent_SchoolRef_IdAndActiveTrue(school.getId());
        Set<Long> absentEnrollmentIds = absentEnrollmentIdsToday(enrollments);

        Map<TransporterProfile, List<Enrollment>> byTransporter = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getTransporter, LinkedHashMap::new, Collectors.toList()));

        List<TransporterStatsResponse> result = new ArrayList<>();
        for (Map.Entry<TransporterProfile, List<Enrollment>> entry : byTransporter.entrySet()) {
            List<Enrollment> ofTransporter = entry.getValue();
            int confirmed = 0;
            int absent = 0;
            for (Enrollment e : ofTransporter) {
                if (absentEnrollmentIds.contains(e.getId())) {
                    absent++;
                } else {
                    confirmed++;
                }
            }
            result.add(TransporterStatsResponse.from(entry.getKey(), ofTransporter.size(), confirmed, absent));
        }
        return result;
    }

    @Override
    public List<StudentDetailResponse> getStudents() {
        School school = currentSchool();
        List<Enrollment> enrollments = enrollmentRepository.findByDependent_SchoolRef_IdAndActiveTrue(school.getId());
        Set<Long> absentEnrollmentIds = absentEnrollmentIdsToday(enrollments);

        List<StudentDetailResponse> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Dependent dependent = e.getDependent();
            GuardianProfile guardian = dependent.getGuardian();
            TransporterProfile transporter = e.getTransporter();
            result.add(new StudentDetailResponse(
                    dependent.getId(),
                    dependent.getName(),
                    dependent.getPhotoUrl(),
                    guardian.getId(),
                    guardian.getUser().getName(),
                    guardian.getUser().getPhone(),
                    transporter.getId(),
                    transporter.getUser().getName(),
                    guardian.getNeighborhood(),
                    absentEnrollmentIds.contains(e.getId()) ? "absent" : "confirmed"));
        }
        return result;
    }

    @Override
    public List<ActivityItemResponse> getRecentActivity() {
        School school = currentSchool();
        List<Enrollment> enrollments = enrollmentRepository.findByDependent_SchoolRef_IdAndActiveTrue(school.getId());
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).toList();
        List<Long> transporterIds = enrollments.stream()
                .map(e -> e.getTransporter().getId())
                .distinct()
                .toList();

        LocalDate today = LocalDate.now(ZONE);
        List<ActivityItemResponse> items = new ArrayList<>();

        if (!enrollmentIds.isEmpty()) {
            Map<Long, Enrollment> byId = enrollments.stream()
                    .collect(Collectors.toMap(Enrollment::getId, e -> e));
            List<Absence> recentAbsences = absenceRepository.findByEnrollmentIdInAndDateBetweenOrderByCreatedAtDesc(
                    enrollmentIds, today.minusDays(7), today);
            for (Absence absence : recentAbsences) {
                Enrollment e = byId.get(absence.getEnrollment().getId());
                if (e == null) {
                    continue;
                }
                items.add(new ActivityItemResponse(
                        "absence",
                        e.getTransporter().getUser().getName(),
                        e.getDependent().getName(),
                        absence.getCreatedAt()));
            }
        }

        Instant recentSince = today.minusDays(30).atStartOfDay(ZONE).toInstant();
        for (Enrollment e : enrollments) {
            if (e.getCreatedAt() != null && e.getCreatedAt().isAfter(recentSince)) {
                items.add(new ActivityItemResponse(
                        "new_link",
                        e.getTransporter().getUser().getName(),
                        e.getDependent().getName(),
                        e.getCreatedAt()));
            }
        }

        if (!transporterIds.isEmpty()) {
            Map<Long, String> transporterNames = enrollments.stream()
                    .collect(Collectors.toMap(
                            e -> e.getTransporter().getId(), e -> e.getTransporter().getUser().getName(),
                            (a, b) -> a));
            for (Review review : reviewRepository.findByTransporterIdInOrderByCreatedAtDesc(transporterIds)) {
                if (review.getCreatedAt() == null || review.getCreatedAt().isBefore(recentSince)) {
                    continue;
                }
                items.add(new ActivityItemResponse(
                        "review",
                        transporterNames.get(review.getTransporter().getId()),
                        null,
                        review.getCreatedAt()));
            }
        }

        return items.stream()
                .sorted(Comparator.comparing(ActivityItemResponse::timestamp).reversed())
                .limit(10)
                .toList();
    }

    @Override
    public List<MonthlyTrendPointResponse> getMonthlyTrend() {
        School school = currentSchool();
        List<Enrollment> enrollments = enrollmentRepository.findByDependent_SchoolRef_IdAndActiveTrue(school.getId());

        YearMonth current = YearMonth.now(ZONE);
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        List<YearMonth> lastSixMonths = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            lastSixMonths.add(month);
            counts.put(month, 0L);
        }

        for (Enrollment e : enrollments) {
            if (e.getCreatedAt() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(e.getCreatedAt().atZone(ZONE));
            counts.computeIfPresent(month, (k, v) -> v + 1);
        }

        return lastSixMonths.stream()
                .map(m -> new MonthlyTrendPointResponse(m.getYear(), m.getMonthValue(), counts.get(m)))
                .toList();
    }

    private Set<Long> absentEnrollmentIdsToday(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return Set.of();
        }
        LocalDate today = LocalDate.now(ZONE);
        List<Long> ids = enrollments.stream().map(Enrollment::getId).toList();
        return absenceRepository.findByEnrollmentIdInAndDate(ids, today).stream()
                .map(a -> a.getEnrollment().getId())
                .collect(Collectors.toSet());
    }

    private School currentSchool() {
        Long userId = currentUserProvider.requireUserId();
        return schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Nenhuma escola vinculada a esta conta."));
    }
}
