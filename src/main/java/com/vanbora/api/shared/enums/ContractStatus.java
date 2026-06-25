package com.vanbora.api.shared.enums;

/** Estado de um contrato de transporte entre responsável e transportador. */
public enum ContractStatus {
    /** Liberado pelo transportador, aguardando assinatura do responsável. */
    PENDING_SIGNATURE,
    /** Assinado e com pagamento configurado — matrícula ativa. */
    ACTIVE,
    /** Cancelado antes ou depois da assinatura. */
    CANCELLED
}
