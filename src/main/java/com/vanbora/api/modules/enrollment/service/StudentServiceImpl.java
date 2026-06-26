package com.vanbora.api.modules.enrollment.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.dto.StudentResponse;
import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação da lista de alunos. */
@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TransporterProfileRepository transporterRepository;
    private final AbsenceRepository absenceRepository;

    public StudentServiceImpl(EnrollmentRepository enrollmentRepository,
                              TransporterProfileRepository transporterRepository,
                              AbsenceRepository absenceRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.transporterRepository = transporterRepository;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public List<StudentResponse> listMyStudents(Long transporterUserId, FinanceStatus financeStatus) {
        TransporterProfile transporter = transporterRepository.findByUserId(transporterUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", transporterUserId));

        LocalDate today = LocalDate.now();
        boolean weekend =
                today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;

        return enrollmentRepository.findByTransporterId(transporter.getId()).stream()
                .filter(e -> financeStatus == null || e.getFinanceStatus() == financeStatus)
                .map(e -> StudentResponse.from(e, weekend || isPresentToday(e, today)))
                .toList();
    }

    /** Presente hoje = não há falta avisada para a data (default: presente). */
    private boolean isPresentToday(Enrollment enrollment, LocalDate today) {
        return !absenceRepository.existsByEnrollmentIdAndDate(enrollment.getId(), today);
    }
}
