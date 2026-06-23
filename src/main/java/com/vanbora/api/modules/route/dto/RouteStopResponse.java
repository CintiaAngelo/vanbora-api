package com.vanbora.api.modules.route.dto;

import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.shared.enums.RouteStopStatus;

/** Parada da rota do dia. */
public record RouteStopResponse(
        Long id,
        String label,
        String address,
        RouteStopStatus status,
        int position
) {
    public static RouteStopResponse from(RouteStop stop) {
        return new RouteStopResponse(
                stop.getId(), stop.getLabel(), stop.getAddress(), stop.getStatus(), stop.getPosition());
    }
}
