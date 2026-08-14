package com.vanbora.api.modules.finance.dto;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pagamento (vencido ou pendente) com os dados do aluno/responsável, para o transportador
 * ver e cobrar. Usado tanto pela lista de "Vencido" quanto pela de "Pendente" — quantos dias
 * de atraso/antecedência é uma conta simples sobre {@code dueDate}, feita no app.
 */
public record PaymentContactResponse(
        Long paymentId,
        Long dependentId,
        String studentName,
        String guardianName,
        String guardianPhone,
        BigDecimal amount,
        LocalDate dueDate
) {
    public static PaymentContactResponse from(Payment payment) {
        Dependent dependent = payment.getEnrollment().getDependent();
        User guardianUser = dependent.getGuardian().getUser();
        return new PaymentContactResponse(
                payment.getId(),
                dependent.getId(),
                dependent.getName(),
                guardianUser.getName(),
                guardianUser.getPhone(),
                payment.getAmount(),
                payment.getDueDate());
    }
}
