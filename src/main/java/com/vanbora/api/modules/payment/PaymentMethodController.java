package com.vanbora.api.modules.payment;

import com.vanbora.api.modules.payment.dto.PaymentMethodResponse;
import com.vanbora.api.modules.payment.dto.SavePaymentMethodRequest;
import com.vanbora.api.modules.payment.service.PaymentMethodService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Meios de pagamento do responsável autenticado. */
@RestController
@RequestMapping("/api/guardians/me/payment-methods")
@PreAuthorize("hasRole('GUARDIAN')")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentMethodController(PaymentMethodService paymentMethodService,
                                   CurrentUserProvider currentUserProvider) {
        this.paymentMethodService = paymentMethodService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<PaymentMethodResponse> list() {
        return paymentMethodService.list(currentUserProvider.requireUserId());
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> save(@Valid @RequestBody SavePaymentMethodRequest request) {
        PaymentMethodResponse response =
                paymentMethodService.save(currentUserProvider.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        paymentMethodService.remove(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
