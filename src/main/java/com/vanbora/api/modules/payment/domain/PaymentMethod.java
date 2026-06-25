package com.vanbora.api.modules.payment.domain;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Meio de pagamento salvo pelo responsável. Por segurança (offload de PCI) guarda
 * APENAS o token opaco do gateway, a bandeira e os últimos 4 dígitos — nunca o
 * número completo do cartão nem o CVV.
 */
@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethodType type;

    /** Token opaco devolvido pelo gateway (representa o cartão). */
    @Column(name = "gateway_token", nullable = false)
    private String gatewayToken;

    @Column(nullable = false, length = 20)
    private String brand;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(nullable = false)
    private boolean active = true;
}
