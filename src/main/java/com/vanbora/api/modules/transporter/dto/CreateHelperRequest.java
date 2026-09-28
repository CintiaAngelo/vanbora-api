package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.monitor.dto.MonitorAccessRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Cadastro de um ajudante/monitor do transportador.
 *
 * @param access dados de acesso ao app (só para a função Monitor); nulo = sem acesso
 */
public record CreateHelperRequest(
        @NotBlank String name,
        @NotBlank String role,
        @Valid MonitorAccessRequest access
) {
}
