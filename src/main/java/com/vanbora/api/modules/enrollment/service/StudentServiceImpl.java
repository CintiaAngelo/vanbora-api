package com.vanbora.api.modules.enrollment.service;

import com.vanbora.api.modules.enrollment.dto.StudentResponse;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
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

        return enrollmentRepository.findByTransporterId(transporter.getId()).stream()
                .filter(e -> financeStatus == null || e.getFinanceStatus() == financeStatus)
                .map(StudentResponse::from)
                .toList();
    }
}
