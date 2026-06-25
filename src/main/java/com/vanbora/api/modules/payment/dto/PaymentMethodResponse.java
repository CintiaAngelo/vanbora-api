package com.vanbora.api.modules.payment.dto;

import com.vanbora.api.modules.payment.domain.PaymentMethod;
import com.vanbora.api.shared.enums.PaymentMethodType;

/** Meio de pagamento salvo, exibido ao responsável (sem dados sensíveis). */
public record PaymentMethodResponse(
        Long id,
        PaymentMethodType type,
        String brand,
        String last4
) {
    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(), method.getType(), method.getBrand(), method.getLast4());
    }
}
