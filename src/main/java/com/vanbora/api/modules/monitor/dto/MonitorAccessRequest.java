package com.vanbora.api.modules.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de acesso ao app de um monitor, informados pelo transportador.
 *
 * @param password senha provisória. Obrigatória ao criar o acesso; na edição, nula/vazia
 *                 mantém a senha atual (preencher redefine).
 */
public record MonitorAccessRequest(
        @NotBlank String email,
        @NotBlank String phone,
        @Size(max = 72) String password
) {
}
