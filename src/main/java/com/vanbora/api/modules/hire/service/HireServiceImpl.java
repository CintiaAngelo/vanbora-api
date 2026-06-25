package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notification.NotificationService;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos casos de uso de contratação. */
@Service
public class HireServiceImpl implements HireService {

    private final HireRequestRepository hireRequestRepository;
    private final GuardianProfileRepository guardianRepository;
    private final TransporterProfileRepository transporterRepository;
    private final DependentRepository dependentRepository;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    public HireServiceImpl(
            HireRequestRepository hireRequestRepository,
            GuardianProfileRepository guardianRepository,
            TransporterProfileRepository transporterRepository,
            DependentRepository dependentRepository,
            ContractRepository contractRepository,
            NotificationService notificationService) {
        this.hireRequestRepository = hireRequestRepository;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.dependentRepository = dependentRepository;
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
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

        if (request.proposedFee() != null) {
            if (!transporter.isAcceptsProposals()) {
                throw new BusinessException("Este transportador não aceita propostas de valor.");
            }
            hire.setProposedFee(request.proposedFee());
        }

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

        // Liberar contrato: cria o contrato pendente e notifica o responsável.
        // A matrícula só nasce quando o responsável assina (ContractService.sign).
        Contract contract = new Contract();
        contract.setHireRequest(hire);
        contract.setGuardian(hire.getGuardian());
        contract.setTransporter(hire.getTransporter());
        contract.setDependent(hire.getDependent());
        // Usa a proposta aceita pelo transportador, ou o valor de tabela quando não há proposta.
        contract.setMonthlyFee(hire.getProposedFee() != null
                ? hire.getProposedFee()
                : hire.getTransporter().getBaseMonthlyFee());
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setSignatureToken(UUID.randomUUID().toString().replace("-", ""));
        contractRepository.save(contract);

        notificationService.notifyContractReleased(contract);
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
