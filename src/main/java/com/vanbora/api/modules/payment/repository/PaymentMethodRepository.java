package com.vanbora.api.modules.payment.repository;

import com.vanbora.api.modules.payment.domain.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByGuardianIdAndActiveTrueOrderByIdDesc(Long guardianId);

    Optional<PaymentMethod> findByIdAndGuardianIdAndActiveTrue(Long id, Long guardianId);
}
