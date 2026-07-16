package com.vanbora.api.shared.enums;

/** Plano de contratação escolhido pelo responsável ao assinar o contrato. */
public enum PlanType {
    /** Mensal: mais caro, sem fidelidade — cancela a qualquer momento sem multa. */
    MONTHLY,
    /** Anual: valor do ano pago à vista — reembolso proporcional no cancelamento. */
    ANNUAL,
    /** Parcelado: mensalidade menor com fidelidade — multa se cancelar antes do prazo. */
    INSTALLMENT
}
