package com.vanbora.api.modules.payment.service;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.payment.domain.PaymentMethod;
import com.vanbora.api.modules.payment.dto.PaymentMethodResponse;
import com.vanbora.api.modules.payment.dto.SavePaymentMethodRequest;
import com.vanbora.api.modules.payment.repository.PaymentMethodRepository;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final GuardianProfileRepository guardianRepository;

    public PaymentMethodServiceImpl(PaymentMethodRepository paymentMethodRepository,
                                    GuardianProfileRepository guardianRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.guardianRepository = guardianRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list(Long userId) {
        GuardianProfile guardian = resolveGuardian(userId);
        return paymentMethodRepository.findByGuardianIdAndActiveTrueOrderByIdDesc(guardian.getId()).stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PaymentMethodResponse save(Long userId, SavePaymentMethodRequest request) {
        GuardianProfile guardian = resolveGuardian(userId);
        PaymentMethod method = new PaymentMethod();
        method.setGuardian(guardian);
        method.setType(request.type());
        method.setGatewayToken(request.token());
        method.setBrand(request.brand());
        method.setLast4(request.last4());
        method.setActive(true);
        return PaymentMethodResponse.from(paymentMethodRepository.save(method));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long paymentMethodId) {
        GuardianProfile guardian = resolveGuardian(userId);
        PaymentMethod method = paymentMethodRepository
                .findByIdAndGuardianIdAndActiveTrue(paymentMethodId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Meio de pagamento", paymentMethodId));
        method.setActive(false);
        paymentMethodRepository.save(method);
    }

    private GuardianProfile resolveGuardian(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }
}
