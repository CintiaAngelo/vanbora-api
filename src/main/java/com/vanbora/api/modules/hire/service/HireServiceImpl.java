package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos casos de uso de contratação. */
@Service
public class HireServiceImpl implements HireService {

    private final HireRequestRepository hireRequestRepository;
    private final GuardianProfileRepository guardianRepository;
    private final TransporterProfileRepository transporterRepository;
    private final DependentRepository dependentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public HireServiceImpl(
            HireRequestRepository hireRequestRepository,
            GuardianProfileRepository guardianRepository,
            TransporterProfileRepository transporterRepository,
            DependentRepository dependentRepository,
            EnrollmentRepository enrollmentRepository) {
        this.hireRequestRepository = hireRequestRepository;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.dependentRepository = dependentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public HireRequestResponse requestHire(Long guardianUserId, CreateHireRequest request) {
        GuardianProfile guardian = resolveGuardian(guardianUserId);
        TransporterProfile transporter = transporterRepository.findById(request.transporterId())
                .orElseThrow(() -> ResourceNotFoundException.of("Transportador", request.transporterId()));
        Dependent dependent = dependentRepository.findById(request.dependentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Dependente", request.dependentId()));

        if (!dependent.getGuardian().getId().equals(guardian.getId())) {
            throw new BusinessException("O dependente informado não pertence a este responsável.");
        }

        HireRequest hire = new HireRequest();
        hire.setGuardian(guardian);
        hire.setTransporter(transporter);
        hire.setDependent(dependent);
        hire.setStatus(HireStatus.PENDING);

        return HireRequestResponse.from(hireRequestRepository.save(hire));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HireRequestResponse> listPendingForTransporter(Long transporterUserId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        return hireRequestRepository
                .findByTransporterIdAndStatus(transporter.getId(), HireStatus.PENDING).stream()
                .map(HireRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void accept(Long transporterUserId, Long hireRequestId) {
        HireRequest hire = requireOwnedRequest(transporterUserId, hireRequestId);
        hire.setStatus(HireStatus.ACCEPTED);

        Enrollment enrollment = new Enrollment();
        enrollment.setTransporter(hire.getTransporter());
        enrollment.setDependent(hire.getDependent());
        enrollment.setMonthlyFee(hire.getTransporter().getBaseMonthlyFee());
        enrollment.setActive(true);
        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void reject(Long transporterUserId, Long hireRequestId) {
        HireRequest hire = requireOwnedRequest(transporterUserId, hireRequestId);
        hire.setStatus(HireStatus.REJECTED);
    }

    private HireRequest requireOwnedRequest(Long transporterUserId, Long hireRequestId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        HireRequest hire = hireRequestRepository.findById(hireRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Solicitação", hireRequestId));
        if (!hire.getTransporter().getId().equals(transporter.getId())) {
            throw new BusinessException("Esta solicitação não pertence ao transportador autenticado.");
        }
        if (hire.getStatus() != HireStatus.PENDING) {
            throw new BusinessException("Esta solicitação já foi respondida.");
        }
        return hire;
    }

    private GuardianProfile resolveGuardian(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }

    private TransporterProfile resolveTransporter(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }
}
