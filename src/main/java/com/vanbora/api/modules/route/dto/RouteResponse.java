package com.vanbora.api.modules.route.dto;

import java.util.List;

/** Rota do dia do transportador: paradas ordenadas + (opcional) o trajeto seguindo ruas. */
public record RouteResponse(
        List<RouteStopResponse> stops,
        /**
         * Sequência de pontos [latitude, longitude] do trajeto seguindo ruas de verdade
         * (via roteamento real). Null quando o roteamento não está disponível (sem chave
         * configurada ou a chamada falhou) — quem consome cai para linha reta entre os
         * pontos, como antes.
         */
        List<double[]> routeGeometry
) {
}
