package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.dto.CreateHireRequest;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notification.NotificationService;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final ContractTextBuilder contractTextBuilder;

    public HireServiceImpl(
            HireRequestRepository hireRequestRepository,
            GuardianProfileRepository guardianRepository,
            TransporterProfileRepository transporterRepository,
            DependentRepository dependentRepository,
            ContractRepository contractRepository,
            EnrollmentRepository enrollmentRepository,
            NotificationService notificationService,
            PushNotificationService pushNotificationService,
            ContractTextBuilder contractTextBuilder) {
        this.hireRequestRepository = hireRequestRepository;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.dependentRepository = dependentRepository;
        this.contractRepository = contractRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
        this.contractTextBuilder = contractTextBuilder;
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

        // Bloqueio por contrato ativo: cancele a assinatura atual antes de contratar outro.
        enrollmentRepository
                .findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(dependent.getId(), guardian.getId())
                .ifPresent(active -> {
                    throw new BusinessException(
                            dependent.getName() + " já tem um transporte ativo com "
                            + active.getTransporter().getUser().getName()
                            + ". Cancele a assinatura atual antes de contratar outro transportador.");
                });

        // Bloqueio por dependente: só uma solicitação pendente por vez para o mesmo aluno.
        Optional<HireRequest> existing = hireRequestRepository
                .findFirstByGuardianIdAndDependentIdAndStatusOrderByIdDesc(
                        guardian.getId(), dependent.getId(), HireStatus.PENDING);
        if (existing.isPresent()) {
            HireRequest pending = existing.get();
            if (pending.isOverdue()) {
                // Expira a vencida de forma preguiçosa e segue, liberando uma nova.
                pending.setStatus(HireStatus.EXPIRED);
                hireRequestRepository.save(pending);
            } else {
                throw new BusinessException(
                        "Já existe uma solicitação pendente para " + dependent.getName()
                        + " com " + pending.getTransporter().getUser().getName()
                        + ". Cancele-a antes de enviar para outro transportador.");
            }
        }

        HireRequest hire = new HireRequest();
        hire.setGuardian(guardian);
        hire.setTransporter(transporter);
        hire.setDependent(dependent);
        hire.setStatus(HireStatus.PENDING);
        hire.setExpiresAt(Instant.now().plus(HireRequest.TTL_DAYS, ChronoUnit.DAYS));

        if (request.proposedFee() != null) {
            if (!transporter.isAcceptsProposals()) {
                throw new BusinessException("Este transportador não aceita propostas de valor.");
            }
            hire.setProposedFee(request.proposedFee());
        }

        HireRequest saved = hireRequestRepository.save(hire);

        // Avisa o transportador da nova solicitação.
        pushNotificationService.sendToUser(
                transporter.getUser().getId(),
                "Nova solicitação de contratação",
                guardian.getUser().getName() + " quer contratar você para " + dependent.getName() + ".",
                Map.of("type", "HIRE_REQUEST"));

        return HireRequestResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HireRequestResponse> listPendingForTransporter(Long transporterUserId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        return hireRequestRepository
                .findByTransporterIdAndStatus(transporter.getId(), HireStatus.PENDING).stream()
                // Esconde as vencidas que o job ainda não varreu (serão expiradas em seguida).
                .filter(h -> !h.isOverdue())
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
        // Congela os demais planos ofertados (anual/parcelado) para o responsável escolher ao assinar.
        contract.setAnnualPlanFee(hire.getTransporter().getAnnualPlanFee());
        contract.setInstallmentMonthlyFee(hire.getTransporter().getInstallmentMonthlyFee());
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setSignatureToken(UUID.randomUUID().toString().replace("-", ""));
        // Congela o texto (modelo do transportador ou padrão) que o responsável assinará.
        contract.setContractText(contractTextBuilder.build(contract));
        contractRepository.save(contract);

        notificationService.notifyContractReleased(contract);
        // Avisa o responsável que o contrato foi liberado para assinatura.
        pushNotificationService.sendToUser(
                hire.getGuardian().getUser().getId(),
                "Solicitação aceita!",
                hire.getTransporter().getUser().getName()
                        + " aceitou. Toque para revisar e assinar o contrato.",
                Map.of("type", "CONTRACT_RELEASED", "contractId", contract.getId()));
    }

    @Override
    @Transactional
    public void reject(Long transporterUserId, Long hireRequestId) {
        HireRequest hire = requireOwnedRequest(transporterUserId, hireRequestId);
        hire.setStatus(HireStatus.REJECTED);

        // Avisa o responsável da recusa.
        pushNotificationService.sendToUser(
                hire.getGuardian().getUser().getId(),
                "Solicitação recusada",
                hire.getTransporter().getUser().getName()
                        + " não aceitou sua solicitação para " + hire.getDependent().getName() + ".",
                Map.of("type", "HIRE_REJECTED"));
    }

    @Override
    @Transactional
    public void cancelHireRequest(Long guardianUserId, Long hireRequestId) {
        HireRequest hire = requireOwnGuardianRequest(guardianUserId, hireRequestId);
        if (hire.getStatus() != HireStatus.PENDING) {
            throw new BusinessException("Só é possível cancelar uma solicitação que ainda está pendente.");
        }
        hire.setStatus(HireStatus.CANCELLED);
        hireRequestRepository.save(hire);

        // Avisa o transportador que a solicitação foi cancelada (some da lista dele).
        pushNotificationService.sendToUser(
                hire.getTransporter().getUser().getId(),
                "Solicitação cancelada",
                hire.getGuardian().getUser().getName()
                        + " cancelou a solicitação de " + hire.getDependent().getName() + ".",
                Map.of("type", "HIRE_CANCELLED"));
    }

    @Override
    @Transactional
    public void dismissHireRequest(Long guardianUserId, Long hireRequestId) {
        HireRequest hire = requireOwnGuardianRequest(guardianUserId, hireRequestId);
        hire.setGuardianDismissedAt(Instant.now());
        hireRequestRepository.save(hire);
    }

    @Override
    @Transactional
    public int expireOverdue() {
        int count = 0;
        for (HireRequest hire : hireRequestRepository.findByStatus(HireStatus.PENDING)) {
            if (hire.isOverdue()) {
                hire.setStatus(HireStatus.EXPIRED);
                // Avisa o responsável que a solicitação expirou sem resposta.
                pushNotificationService.sendToUser(
                        hire.getGuardian().getUser().getId(),
                        "Solicitação expirada",
                        "Sua solicitação para " + hire.getDependent().getName()
                                + " expirou sem resposta de " + hire.getTransporter().getUser().getName()
                                + ". Você pode buscar outro transportador.",
                        Map.of("type", "HIRE_EXPIRED"));
                count++;
            }
        }
        return count;
    }

    private HireRequest requireOwnGuardianRequest(Long guardianUserId, Long hireRequestId) {
        GuardianProfile guardian = resolveGuardian(guardianUserId);
        HireRequest hire = hireRequestRepository.findById(hireRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Solicitação", hireRequestId));
        if (!hire.getGuardian().getId().equals(guardian.getId())) {
            throw new BusinessException("Esta solicitação não pertence a este responsável.");
        }
        return hire;
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
