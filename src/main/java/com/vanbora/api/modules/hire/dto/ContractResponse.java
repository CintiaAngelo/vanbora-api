package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.shared.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato exibido ao responsável (resumo dos termos + estado). */
public record ContractResponse(
        Long id,
        ContractStatus status,
        String transporterName,
        String studentName,
        String school,
        BigDecimal monthlyFee,
        Instant signedAt
) {
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getStatus(),
                contract.getTransporter().getUser().getName(),
                contract.getDependent().getName(),
                contract.getDependent().getSchool(),
                contract.getMonthlyFee(),
                contract.getSignedAt());
    }
}
