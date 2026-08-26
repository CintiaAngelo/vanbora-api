package com.vanbora.api.modules.transporter.dto;

import java.util.List;

/**
 * Nova ordem das fotos do veículo: {@code order[k]} é o índice ATUAL da foto que deve
 * passar a ocupar a posição {@code k}. Deve ser uma permutação de {@code [0, tamanho)}.
 */
public record VehiclePhotoOrderRequest(List<Integer> order) {
}
