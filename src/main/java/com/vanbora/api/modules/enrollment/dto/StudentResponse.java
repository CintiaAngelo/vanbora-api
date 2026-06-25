package com.vanbora.api.modules.enrollment.dto;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.shared.enums.FinanceStatus;

/** Aluno transportado, exibido na lista de alunos do transportador. */
public record StudentResponse(
        Long id,
        String name,
        String guardianName,
        String school,
        String neighborhood,
        FinanceStatus financeStatus,
        boolean presentToday
) {
    public static StudentResponse from(Enrollment e, boolean presentToday) {
        var dependent = e.getDependent();
        var guardianUser = dependent.getGuardian().getUser();
        return new StudentResponse(
                e.getId(),
                dependent.getName(),
                guardianUser.getName(),
                dependent.getSchool(),
                dependent.getGuardian().getNeighborhood(),
                e.getFinanceStatus(),
                presentToday);
    }
}
