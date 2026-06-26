package com.vanbora.api.modules.transporter.dto;

/** Modelo de contrato (apenas texto) do transportador. Vazio/nulo limpa o modelo. */
public record UpdateContractTemplateRequest(
        String template
) {
}
