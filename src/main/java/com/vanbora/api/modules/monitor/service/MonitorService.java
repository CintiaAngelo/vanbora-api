package com.vanbora.api.modules.monitor.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.monitor.dto.MonitorProfileResponse;
import com.vanbora.api.modules.monitor.dto.MonitorStudentResponse;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.service.TripAccessService;
import com.vanbora.api.modules.user.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Visões do próprio monitor, sempre recortadas pelos percursos em que ele está escalado. */
@Service
@Transactional(readOnly = true)
public class MonitorService {

    private final TripAccessService access;
    private final AbsenceRepository absenceRepository;

    public MonitorService(TripAccessService access, AbsenceRepository absenceRepository) {
        this.access = access;
        this.absenceRepository = absenceRepository;
    }

    public MonitorProfileResponse profile(User user) {
        Helper helper = access.requireActiveMonitor(user);
        TransporterProfile transporter = helper.getTransporter();
        List<MonitorProfileResponse.TripRef> trips = tripsOf(user).stream()
                .map(t -> new MonitorProfileResponse.TripRef(t.getId(), t.getName()))
                .toList();
        return new MonitorProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                helper.getPhotoUrl(),
                transporter.getUser().getName(),
                transporter.getUser().getPhone(),
                transporter.getPlate(),
                trips);
    }

    /**
     * Alunos ATIVOS dos percursos do monitor (união, sem repetição). Um monitor sem percurso
     * não vê aluno nenhum — o acesso nasce da escala, nunca do simples vínculo ao transportador.
     */
    public List<MonitorStudentResponse> students(User user) {
        LocalDate today = LocalDate.now();
        boolean weekend = today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;

        Map<Long, Enrollment> enrollments = new LinkedHashMap<>();
        Map<Long, List<String>> tripNames = new LinkedHashMap<>();
        for (Trip trip : tripsOf(user)) {
            for (Enrollment e : access.activeEnrollmentsOf(trip)) {
                Long dependentId = e.getDependent().getId();
                enrollments.putIfAbsent(dependentId, e);
                tripNames.computeIfAbsent(dependentId, k -> new ArrayList<>()).add(trip.getName());
            }
        }
        return enrollments.values().stream()
                .map(e -> {
                    var dependent = e.getDependent();
                    boolean present = weekend || !absenceRepository.existsByEnrollmentIdAndDate(e.getId(), today);
                    return new MonitorStudentResponse(
                            dependent.getId(),
                            dependent.getName(),
                            dependent.getSchool(),
                            dependent.getGuardian().getUser().getName(),
                            dependent.getGuardian().getNeighborhood(),
                            present,
                            tripNames.get(dependent.getId()));
                })
                .toList();
    }

    private List<Trip> tripsOf(User user) {
        return access.tripsOf(access.requireProfessional(user));
    }
}
