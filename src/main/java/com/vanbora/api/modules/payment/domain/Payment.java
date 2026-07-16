package com.vanbora.api.modules.payment.domain;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.PaymentKind;
import com.vanbora.api.shared.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mensalidade de uma matrícula em um mês de referência. */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth; // formato YYYY-MM

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** Natureza do lançamento. Nullable (ddl-auto em tabela populada); null ⇒ MENSALIDADE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentKind kind = PaymentKind.MENSALIDADE;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDate paidAt;
}
