package com.vanbora.api.modules.payment.gateway;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Adaptador do Mercado Pago. Ativado por {@code vanbora.payments.provider=mercadopago}
 * (não é o default). Mantém as regras de negócio intactas — só troca o provedor.
 *
 * <p>Observação de PCI: a tokenização do cartão acontece no app (SDK do Mercado
 * Pago), então aqui só trafega o token; o número do cartão nunca chega ao backend.
 *
 * <p>Scaffold: a chamada real à API está delineada e pode exigir ajustes finos
 * (payment_method_id, payer, etc.) conforme a conta. Requer access token válido.
 */
@Service
@ConditionalOnProperty(name = "vanbora.payments.provider", havingValue = "mercadopago")
public class MercadoPagoPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentGateway.class);
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments";

    private final RestClient restClient;

    public MercadoPagoPaymentGateway(
            @Value("${vanbora.payments.mercadopago.access-token:}") String accessToken) {
        this.restClient = RestClient.builder()
                .baseUrl(PAYMENTS_URL)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    @Override
    public String provider() {
        return "mercadopago";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChargeResult charge(ChargeRequest request) {
        // Payload mínimo de um pagamento com cartão tokenizado no cliente.
        Map<String, Object> body = Map.of(
                "transaction_amount", request.amount(),
                "token", request.token(),
                "installments", 1,
                "description", request.description());
        try {
            Map<String, Object> response = restClient.post()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String status = response != null ? String.valueOf(response.get("status")) : "unknown";
            String id = response != null ? String.valueOf(response.get("id")) : null;
            boolean approved = "approved".equals(status);
            return new ChargeResult(approved, id, status);
        } catch (Exception e) {
            log.error("Falha ao cobrar no Mercado Pago", e);
            return new ChargeResult(false, null, "Erro no gateway: " + e.getMessage());
        }
    }
}
