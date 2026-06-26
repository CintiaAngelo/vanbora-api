package com.vanbora.api.modules.transporter.dto;

import java.util.List;

/**
 * Opções de filtro da tela de busca: escolas e bairros que JÁ estão armazenados
 * nas áreas de atendimento dos transportadores, em ordem alfabética.
 */
public record ServiceAreaOptionsResponse(
        List<String> schools,
        List<String> neighborhoods
) {
}
