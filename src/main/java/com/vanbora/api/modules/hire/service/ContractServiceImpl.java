package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.dto.CancellationPreviewResponse;
import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.notification.NotificationService;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.domain.PaymentMethod;
import com.vanbora.api.modules.payment.gateway.PaymentGateway;
import com.vanbora.api.modules.payment.gateway.PaymentGateway.ChargeRequest;
import com.vanbora.api.modules.payment.gateway.PaymentGateway.ChargeResult;
import com.vanbora.api.modules.payment.repository.PaymentMethodRepository;
import com.vanbora.api.modules.route.service.RouteProvisioningService;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.service.ReviewService;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.PaymentKind;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.PlanType;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.fee.FeeCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
    private final ReviewService reviewService;
    private final ContractTextBuilder contractTextBuilder;
    private final FeeCalculator feeCalculator;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public ContractServiceImpl(ContractRepository contractRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               EnrollmentRepository enrollmentRepository,
                               GuardianProfileRepository guardianRepository,
                               PaymentGateway paymentGateway,
                               RouteProvisioningService routeProvisioningService,
                               ReviewService reviewService,
                               ContractTextBuilder contractTextBuilder,
                               FeeCalculator feeCalculator,
                               NotificationService notificationService,
                               PushNotificationService pushNotificationService) {
        this.contractRepository = contractRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.guardianRepository = guardianRepository;
        this.paymentGateway = paymentGateway;
        this.routeProvisioningService = routeProvisioningService;
        this.reviewService = reviewService;
        this.contractTextBuilder = contractTextBuilder;
        this.feeCalculator = feeCalculator;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
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
    @Transactional
    public ContractResponse get(Long userId, Long contractId) {
        Contract contract = requireOwnContract(userId, contractId);
        // Backfill: contratos liberados antes deste recurso (ou do seed) não têm o texto.
        if (contract.getContractText() == null || contract.getContractText().isBlank()) {
            contract.setContractText(contractTextBuilder.build(contract));
            contractRepository.save(contract);
        }
        return ContractResponse.from(contract);
    }

    @Override
    @Transactional
    public ContractResponse sign(Long userId, Long contractId, Long paymentMethodId, PlanType planType) {
        GuardianProfile guardian = resolveGuardian(userId);
        Contract contract = contractRepository.findByIdAndGuardianId(contractId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Contrato", contractId));

        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BusinessException("Este contrato não está pendente de assinatura.");
        }

        PaymentMethod method = paymentMethodRepository
                .findByIdAndGuardianIdAndActiveTrue(paymentMethodId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Meio de pagamento", paymentMethodId));

        PlanType plan = planType != null ? planType : PlanType.MONTHLY;
        int fidelityMonths = feeCalculator.properties().fidelityMonths();

        // Define a cobrança e a mensalidade de referência da matrícula conforme o plano.
        BigDecimal chargeAmount;
        BigDecimal enrollmentMonthly;
        int fidelity;
        switch (plan) {
            case ANNUAL -> {
                if (contract.getAnnualPlanFee() == null) {
                    throw new BusinessException("O plano anual não está disponível neste contrato.");
                }
                chargeAmount = contract.getAnnualPlanFee();
                enrollmentMonthly = contract.getMonthlyFee();
                fidelity = fidelityMonths;
                contract.setAmountUpfront(chargeAmount);
            }
            case INSTALLMENT -> {
                if (contract.getInstallmentMonthlyFee() == null) {
                    throw new BusinessException("O plano parcelado não está disponível neste contrato.");
                }
                chargeAmount = contract.getInstallmentMonthlyFee();
                enrollmentMonthly = chargeAmount;
                fidelity = fidelityMonths;
            }
            default -> {
                chargeAmount = contract.getMonthlyFee();
                enrollmentMonthly = chargeAmount;
                fidelity = 0;
            }
        }

        // Cobra pelo gateway (token opaco — sem dados do cartão).
        ChargeResult charge = paymentGateway.charge(new ChargeRequest(
                method.getGatewayToken(),
                chargeAmount,
                "Mensalidade VanBora (" + plan + ") - " + contract.getDependent().getName()));

        // Cria a matrícula ativa.
        Enrollment enrollment = new Enrollment();
        enrollment.setTransporter(contract.getTransporter());
        enrollment.setDependent(contract.getDependent());
        enrollment.setMonthlyFee(enrollmentMonthly);
        enrollment.setFinanceStatus(charge.approved() ? FinanceStatus.EM_DIA : FinanceStatus.PENDENTE);
        enrollment.setActive(true);

        // Primeira cobrança (mensalidade ou total anual).
        Payment payment = new Payment();
        payment.setEnrollment(enrollment);
        payment.setReferenceMonth(YearMonth.now().toString()); // yyyy-MM
        payment.setAmount(chargeAmount);
        payment.setKind(PaymentKind.MENSALIDADE);
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

        // Marca o contrato como assinado/ativo, gravando o plano e a fidelidade.
        contract.setEnrollment(enrollment);
        contract.setPlanType(plan);
        contract.setFidelityMonths(fidelity);
        contract.setStartDate(LocalDate.now());
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setSignedAt(Instant.now());
        contractRepository.save(contract);

        return ContractResponse.from(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResponse cancellationPreview(Long userId, Long contractId) {
        Contract contract = requireOwnContract(userId, contractId);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Só é possível cancelar um contrato ativo.");
        }
        CancellationCalc calc = computeCancellation(contract);
        boolean requiresPayment = isPositive(calc.fine());
        return new CancellationPreviewResponse(
                calc.plan(), calc.monthsElapsed(), calc.fidelityActive(),
                calc.fine(), calc.refund(), requiresPayment, buildCancellationMessage(calc));
    }

    @Override
    @Transactional
    public ContractResponse cancel(Long userId, Long contractId, int rating, String comment, Long paymentMethodId) {
        GuardianProfile guardian = resolveGuardian(userId);
        Contract contract = contractRepository.findByIdAndGuardianId(contractId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Contrato", contractId));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Só é possível cancelar um contrato ativo.");
        }

        TransporterProfile transporter = contract.getTransporter();
        CancellationCalc calc = computeCancellation(contract);

        // Avaliação obrigatória no cancelamento (registra + recalcula a média).
        reviewService.addReview(guardian, transporter, rating, comment);

        // Multa (parcelado dentro da fidelidade): cobra pelo gateway antes de encerrar.
        if (isPositive(calc.fine())) {
            if (paymentMethodId == null) {
                throw new BusinessException("Selecione a forma de pagamento da multa para cancelar.");
            }
            PaymentMethod method = paymentMethodRepository
                    .findByIdAndGuardianIdAndActiveTrue(paymentMethodId, guardian.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Meio de pagamento", paymentMethodId));
            ChargeResult charge = paymentGateway.charge(new ChargeRequest(
                    method.getGatewayToken(), calc.fine(),
                    "Multa de rescisão VanBora - " + contract.getDependent().getName()));
            if (!charge.approved()) {
                throw new BusinessException("Não foi possível cobrar a multa de rescisão: " + charge.message());
            }
            contract.setCancellationFine(calc.fine());
        }

        // Reembolso (anual): registra o valor devolvido (gateway simulado processa o estorno).
        if (isPositive(calc.refund())) {
            contract.setRefundAmount(calc.refund());
        }

        // Encerra APENAS o vínculo do dependente deste contrato (não afeta outros filhos).
        Long dependentId = contract.getDependent().getId();
        enrollmentRepository
                .findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(dependentId, guardian.getId())
                .ifPresent(e -> {
                    e.setActive(false);
                    enrollmentRepository.save(e);
                });

        // Tira o aluno da rota/mapa do transportador (não conta mais como aluno dele).
        routeProvisioningService.deprovisionForDependent(transporter.getId(), dependentId);

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancelledAt(Instant.now());
        contractRepository.save(contract);

        // Notifica o transportador automaticamente sobre a rescisão (e-mail simulado + push).
        notificationService.notifyContractCancelled(contract);
        pushNotificationService.sendToUser(
                transporter.getUser().getId(),
                "Contrato cancelado",
                guardian.getUser().getName() + " cancelou o contrato de "
                        + contract.getDependent().getName() + ".",
                Map.of("type", "CONTRACT_CANCELLED", "contractId", contract.getId()));

        return ContractResponse.from(contract);
    }

    /** Calcula multa/reembolso do contrato conforme o plano e o tempo decorrido. */
    private CancellationCalc computeCancellation(Contract contract) {
        PlanType plan = contract.getPlanType() != null ? contract.getPlanType() : PlanType.MONTHLY;
        int fidelity = contract.getFidelityMonths() != null ? contract.getFidelityMonths() : 0;
        int elapsedFloor = monthsElapsedFloor(contract);

        BigDecimal fine = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        boolean fidelityActive = false;

        if (plan == PlanType.INSTALLMENT && fidelity > 0 && elapsedFloor < fidelity
                && contract.getInstallmentMonthlyFee() != null) {
            fidelityActive = true;
            fine = feeCalculator.installmentFine(contract.getInstallmentMonthlyFee(), elapsedFloor);
        } else if (plan == PlanType.ANNUAL && contract.getAmountUpfront() != null) {
            refund = feeCalculator.annualRefund(contract.getAmountUpfront(), monthsUsedCeil(contract));
        }
        return new CancellationCalc(plan, elapsedFloor, fidelityActive, fine, refund);
    }

    private String buildCancellationMessage(CancellationCalc calc) {
        if (isPositive(calc.fine())) {
            return "Este contrato tem fidelidade ativa. Ao cancelar, será cobrada uma multa de "
                    + calc.fine() + ".";
        }
        if (isPositive(calc.refund())) {
            return "Plano anual: você receberá um reembolso proporcional de " + calc.refund() + ".";
        }
        return "Sem multa ou reembolso — o cancelamento é imediato.";
    }

    /** Meses completos desde o início da vigência (base da multa). */
    private int monthsElapsedFloor(Contract contract) {
        LocalDate start = startDate(contract);
        long months = ChronoUnit.MONTHS.between(start, LocalDate.now());
        return (int) Math.max(0, months);
    }

    /** Meses usados arredondando para cima — fração de mês em curso conta como usada (reembolso). */
    private int monthsUsedCeil(Contract contract) {
        LocalDate start = startDate(contract);
        LocalDate now = LocalDate.now();
        long full = ChronoUnit.MONTHS.between(start, now);
        int used = (int) Math.max(0, full);
        if (start.plusMonths(full).isBefore(now)) {
            used += 1;
        }
        return used;
    }

    private LocalDate startDate(Contract contract) {
        if (contract.getStartDate() != null) {
            return contract.getStartDate();
        }
        if (contract.getSignedAt() != null) {
            return LocalDate.ofInstant(contract.getSignedAt(), ZoneId.systemDefault());
        }
        return LocalDate.now();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
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

    /** Resultado do cálculo de rescisão. */
    private record CancellationCalc(
            PlanType plan,
            int monthsElapsed,
            boolean fidelityActive,
            BigDecimal fine,
            BigDecimal refund) {
    }
}
