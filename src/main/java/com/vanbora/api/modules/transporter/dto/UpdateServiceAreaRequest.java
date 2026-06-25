package com.vanbora.api.modules.transporter.dto;

import java.util.List;

/** Edição da área de atendimento (escolas e bairros). Listas nulas = mantém. */
public record UpdateServiceAreaRequest(
        List<String> schools,
        List<String> neighborhoods
) {
}
