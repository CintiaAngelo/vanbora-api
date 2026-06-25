package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.domain.PaymentMethod;
import com.vanbora.api.modules.payment.gateway.PaymentGateway;
import com.vanbora.api.modules.payment.gateway.PaymentGateway.ChargeRequest;
import com.vanbora.api.modules.payment.gateway.PaymentGateway.ChargeResult;
import com.vanbora.api.modules.payment.repository.PaymentMethodRepository;
import com.vanbora.api.modules.route.service.RouteProvisioningService;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GuardianProfileRepository guardianRepository;
    private final PaymentGateway paymentGateway;
    private final RouteProvisioningService routeProvisioningService;

    public ContractServiceImpl(ContractRepository contractRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               EnrollmentRepository enrollmentRepository,
                               GuardianProfileRepository guardianRepository,
                               PaymentGateway paymentGateway,
                               RouteProvisioningService routeProvisioningService) {
        this.contractRepository = contractRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.guardianRepository = guardianRepository;
        this.paymentGateway = paymentGateway;
        this.routeProvisioningService = routeProvisioningService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> listForGuardian(Long userId, ContractStatus status) {
        Long guardianId = resolveGuardian(userId).getId();
        List<Contract> contracts = (status == null)
                ? contractRepository.findByGuardianIdOrderByIdDesc(guardianId)
                : contractRepository.findByGuardianIdAndStatusOrderByIdDesc(guardianId, status);
        return contracts.stream().map(ContractResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse get(Long userId, Long contractId) {
        return ContractResponse.from(requireOwnContract(userId, contractId));
    }

    @Override
    @Transactional
    public ContractResponse sign(Long userId, Long contractId, Long paymentMethodId) {
        GuardianProfile guardian = resolveGuardian(userId);
        Contract contract = contractRepository.findByIdAndGuardianId(contractId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Contrato", contractId));

        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BusinessException("Este contrato não está pendente de assinatura.");
        }

        PaymentMethod method = paymentMethodRepository
                .findByIdAndGuardianIdAndActiveTrue(paymentMethodId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Meio de pagamento", paymentMethodId));

        // Cobra a primeira mensalidade pelo gateway (token opaco — sem dados do cartão).
        ChargeResult charge = paymentGateway.charge(new ChargeRequest(
                method.getGatewayToken(),
                contract.getMonthlyFee(),
                "Mensalidade VanBora - " + contract.getDependent().getName()));

        // Cria a matrícula ativa.
        Enrollment enrollment = new Enrollment();
        enrollment.setTransporter(contract.getTransporter());
        enrollment.setDependent(contract.getDependent());
        enrollment.setMonthlyFee(contract.getMonthlyFee());
        enrollment.setFinanceStatus(charge.approved() ? FinanceStatus.EM_DIA : FinanceStatus.PENDENTE);
        enrollment.setActive(true);

        // Primeiro pagamento do mês corrente.
        Payment payment = new Payment();
        payment.setEnrollment(enrollment);
        payment.setReferenceMonth(YearMonth.now().toString()); // yyyy-MM
        payment.setAmount(contract.getMonthlyFee());
        if (charge.approved()) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDate.now());
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDueDate(LocalDate.now().plusDays(5));
        }
        enrollment.getPayments().add(payment);
        enrollmentRepository.save(enrollment);

        // Coloca o aluno na rota do transportador (parada de embarque + escola, no mapa).
        routeProvisioningService.provisionForEnrollment(enrollment);

        // Marca o contrato como assinado/ativo.
        contract.setEnrollment(enrollment);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setSignedAt(Instant.now());
        contractRepository.save(contract);

        return ContractResponse.from(contract);
    }

    private Contract requireOwnContract(Long userId, Long contractId) {
        Long guardianId = resolveGuardian(userId).getId();
        return contractRepository.findByIdAndGuardianId(contractId, guardianId)
                .orElseThrow(() -> ResourceNotFoundException.of("Contrato", contractId));
    }

    private GuardianProfile resolveGuardian(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }
}
