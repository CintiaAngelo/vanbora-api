package com.vanbora.api.modules.enrollment.service;

import com.vanbora.api.modules.enrollment.domain.Attendance;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.dto.StudentResponse;
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

    public StudentServiceImpl(EnrollmentRepository enrollmentRepository,
                              TransporterProfileRepository transporterRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.transporterRepository = transporterRepository;
    }

    @Override
    public List<StudentResponse> listMyStudents(Long transporterUserId, FinanceStatus financeStatus) {
        TransporterProfile transporter = transporterRepository.findByUserId(transporterUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", transporterUserId));

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        boolean weekend = today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY;

        return enrollmentRepository.findByTransporterId(transporter.getId()).stream()
                .filter(e -> financeStatus == null || e.getFinanceStatus() == financeStatus)
                .map(e -> StudentResponse.from(e, weekend || isPresentOn(e, today)))
                .toList();
    }

    /** Presença do aluno no dia (default: presente quando não há registro). */
    private boolean isPresentOn(Enrollment enrollment, DayOfWeek day) {
        return enrollment.getAttendance().stream()
                .filter(a -> a.getDayOfWeek() == day)
                .findFirst()
                .map(Attendance::isPresent)
                .orElse(true);
    }
}
