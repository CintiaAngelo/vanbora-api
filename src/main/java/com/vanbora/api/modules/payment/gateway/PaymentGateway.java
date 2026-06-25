package com.vanbora.api.modules.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstração de um provedor de pagamentos externo. O VanBora nunca toca em dados
 * sensíveis de cartão: recebe apenas um token opaco (gerado no cliente/gateway) e
 * pede ao gateway para efetuar a cobrança. Trocar de provedor = trocar a
 * implementação, sem mexer nas regras de negócio.
 */
public interface PaymentGateway {

    /** Identificador do provedor ativo (ex.: "simulated", "mercadopago"). */
    String provider();

    /** Efetua uma cobrança usando o token salvo do meio de pagamento. */
    ChargeResult charge(ChargeRequest request);

    /** Dados de uma cobrança a realizar. */
    record ChargeRequest(String token, BigDecimal amount, String description) {}

    /** Resultado de uma cobrança. */
    record ChargeResult(boolean approved, String gatewayPaymentId, String message) {}
}
