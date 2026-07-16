package com.vanbora.api.shared.enums;

/** Natureza de um lançamento financeiro da matrícula. */
public enum PaymentKind {
    /** Mensalidade regular. */
    MENSALIDADE,
    /** Multa de rescisão (fidelidade do parcelado). */
    MULTA,
    /** Reembolso proporcional (cancelamento do plano anual). */
    REEMBOLSO
}
