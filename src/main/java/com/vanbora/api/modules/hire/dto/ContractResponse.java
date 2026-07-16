package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.PlanType;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato exibido ao responsável (resumo dos termos + estado + planos). */
public record ContractResponse(
        Long id,
        ContractStatus status,
        Long transporterId,
        String transporterName,
        Long dependentId,
        String studentName,
        String school,
        BigDecimal monthlyFee,
        BigDecimal annualPlanFee,
        BigDecimal installmentMonthlyFee,
        PlanType planType,
        Integer fidelityMonths,
        BigDecimal cancellationFine,
        BigDecimal refundAmount,
        Instant signedAt,
        String contractText
) {
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getStatus(),
                contract.getTransporter().getId(),
                contract.getTransporter().getUser().getName(),
                contract.getDependent().getId(),
                contract.getDependent().getName(),
                contract.getDependent().getSchool(),
                contract.getMonthlyFee(),
                contract.getAnnualPlanFee(),
                contract.getInstallmentMonthlyFee(),
                contract.getPlanType() != null ? contract.getPlanType() : PlanType.MONTHLY,
                contract.getFidelityMonths(),
                contract.getCancellationFine(),
                contract.getRefundAmount(),
                contract.getSignedAt(),
                contract.getContractText());
    }
}
