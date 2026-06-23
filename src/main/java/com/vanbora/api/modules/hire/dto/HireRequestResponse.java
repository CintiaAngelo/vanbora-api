package com.vanbora.api.modules.hire.dto;

import com.vanbora.api.modules.hire.domain.HireRequest;

/** Solicitação de contratação exibida ao transportador. */
public record HireRequestResponse(
        Long id,
        String guardianName,
        String studentName,
        String school,
        String neighborhood
) {
    public static HireRequestResponse from(HireRequest h) {
        return new HireRequestResponse(
                h.getId(),
                h.getGuardian().getUser().getName(),
                h.getDependent().getName(),
                h.getDependent().getSchool(),
                h.getGuardian().getNeighborhood());
    }
}
