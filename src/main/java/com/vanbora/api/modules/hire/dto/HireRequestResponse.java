package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.modules.hire.domain.HireRequest;
import java.math.BigDecimal;

/** Solicitação de contratação exibida ao transportador, com dados do responsável. */
public record HireRequestResponse(
        Long id,
        String guardianName,
        String guardianPhone,
        String guardianEmail,
        String studentName,
        String school,
        String neighborhood,
        /** Valor de tabela do transportador. */
        BigDecimal baseFee,
        /** Valor proposto pelo responsável (null = sem proposta, usar o de tabela). */
        BigDecimal proposedFee
) {
    public static HireRequestResponse from(HireRequest h) {
        return new HireRequestResponse(
                h.getId(),
                h.getGuardian().getUser().getName(),
                h.getGuardian().getUser().getPhone(),
                h.getGuardian().getUser().getEmail(),
                h.getDependent().getName(),
                h.getDependent().getSchool(),
                h.getGuardian().getNeighborhood(),
                h.getTransporter().getBaseMonthlyFee(),
                h.getProposedFee());
    }
}
