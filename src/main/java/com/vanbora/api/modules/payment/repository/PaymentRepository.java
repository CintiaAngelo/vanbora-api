package com.vanbora.api.modules.payment.repository;

import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.shared.enums.PaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByEnrollmentDependentGuardianIdOrderByReferenceMonthDesc(Long guardianId);

    /** Pagamentos de um dependente específico (visão por dependente). */
    List<Payment> findByEnrollmentDependentIdOrderByReferenceMonthDesc(Long dependentId);

    List<Payment> findByEnrollmentTransporterId(Long transporterId);

    List<Payment> findByEnrollmentTransporterIdAndStatus(Long transporterId, PaymentStatus status);
}
