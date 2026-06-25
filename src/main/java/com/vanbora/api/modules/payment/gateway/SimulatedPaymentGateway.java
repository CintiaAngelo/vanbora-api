package com.vanbora.api.modules.payment.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Gateway simulado (default em desenvolvimento). Aprova todas as cobranças e
 * devolve um id fictício. Permite rodar o fluxo de contratação de ponta a ponta
 * sem credenciais externas. Trocar para o provedor real é só configurar
 * {@code vanbora.payments.provider=mercadopago}.
 */
@Service
@ConditionalOnProperty(name = "vanbora.payments.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    @Override
    public String provider() {
        return "simulated";
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        String paymentId = "sim_pay_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("[PAGAMENTO SIMULADO] Aprovado {} — R$ {} ({}) usando token {}",
                paymentId, request.amount(), request.description(), maskToken(request.token()));
        return new ChargeResult(true, paymentId, "Aprovado (simulado)");
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 6) {
            return "***";
        }
        return token.substring(0, 6) + "…";
    }
}
