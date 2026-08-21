package com.vanbora.api.modules.route.service;

import com.vanbora.api.modules.route.dto.RouteResponse;

/** Casos de uso da rota do dia do transportador. */
public interface RouteService {

    /** Paradas da rota do transportador (em ordem) + trajeto seguindo ruas, se disponível. */
    RouteResponse listForUser(Long userId);

    /**
     * Recalcula a melhor ordem das paradas (vizinho-mais-próximo a partir da
     * posição atual do transportador) mantendo a escola como destino final,
     * persiste e devolve a rota já reordenada + trajeto seguindo ruas, se disponível.
     */
    RouteResponse optimizeForUser(Long userId);
}
