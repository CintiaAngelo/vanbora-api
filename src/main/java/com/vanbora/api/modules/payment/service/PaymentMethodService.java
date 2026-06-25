package com.vanbora.api.modules.payment.service;

import com.vanbora.api.modules.payment.dto.PaymentMethodResponse;
import com.vanbora.api.modules.payment.dto.SavePaymentMethodRequest;
import java.util.List;

/** Casos de uso dos meios de pagamento do responsável. */
public interface PaymentMethodService {

    List<PaymentMethodResponse> list(Long userId);

    PaymentMethodResponse save(Long userId, SavePaymentMethodRequest request);

    void remove(Long userId, Long paymentMethodId);
}
