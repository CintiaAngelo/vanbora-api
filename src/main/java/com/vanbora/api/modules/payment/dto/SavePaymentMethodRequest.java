package com.vanbora.api.modules.payment.dto;

import com.vanbora.api.shared.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Meio de pagamento já tokenizado no cliente. Recebe APENAS o token opaco do
 * gateway + bandeira + últimos 4 dígitos — nunca o número do cartão nem o CVV.
 */
public record SavePaymentMethodRequest(
        @NotBlank String token,
        @NotBlank String brand,
        @NotBlank @Size(min = 4, max = 4) String last4,
        @NotNull PaymentMethodType type
) {
}
