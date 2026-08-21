package com.vanbora.api.modules.route.dto;

import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.shared.enums.RouteStopStatus;

/**
 * Parada da rota do dia, com estimativas de trajeto: distância do ponto anterior,
 * distância acumulada, minutos desde a partida e horário previsto de chegada.
 */
public record RouteStopResponse(
        Long id,
        String label,
        String address,
        RouteStopStatus status,
        int position,
        Double latitude,
        Double longitude,
        Double legDistanceKm,
        Double cumulativeKm,
        Integer etaMinutes,
        String etaClock
) {
    /** Sem estimativas (usado onde o trajeto não é calculado). */
    public static RouteStopResponse from(RouteStop stop) {
        return from(stop, null, null, null, null);
    }

    public static RouteStopResponse from(RouteStop stop, Double legDistanceKm, Double cumulativeKm,
                                         Integer etaMinutes, String etaClock) {
        return new RouteStopResponse(
                stop.getId(), stop.getLabel(), stop.getAddress(), stop.getStatus(),
                stop.getPosition(), stop.getLatitude(), stop.getLongitude(),
                legDistanceKm, cumulativeKm, etaMinutes, etaClock);
    }

    /**
     * Parada de um aluno que avisou falta hoje: status forçado para NOT_GOING (mesmo que a
     * parada em si permaneça GOING no banco — a falta é só de hoje) e sem trecho/ETA, pois
     * ela não faz parte do trajeto de hoje.
     */
    public static RouteStopResponse notGoingToday(RouteStop stop) {
        return new RouteStopResponse(
                stop.getId(), stop.getLabel(), stop.getAddress(), RouteStopStatus.NOT_GOING,
                stop.getPosition(), stop.getLatitude(), stop.getLongitude(),
                null, null, null, null);
    }
}
