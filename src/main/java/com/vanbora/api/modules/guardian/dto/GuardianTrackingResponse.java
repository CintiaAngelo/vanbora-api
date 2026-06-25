package com.vanbora.api.modules.guardian.dto;

import com.vanbora.api.modules.route.dto.RouteStopResponse;
import java.time.Instant;

/**
 * Acompanhamento em tempo real para o responsável. Por privacidade, expõe apenas
 * a posição do transportador, a parada do próprio dependente, a escola (destino)
 * e a ordem de embarque do responsável — nunca dados de outros responsáveis.
 */
public record GuardianTrackingResponse(
        boolean hasTransporter,
        String transporterName,
        String studentName,
        Double latitude,
        Double longitude,
        Instant updatedAt,
        /** Parada de embarque do próprio dependente (ou null). */
        RouteStopResponse myStop,
        /** Parada de destino (escola). */
        RouteStopResponse schoolStop,
        /** Posição do responsável entre os embarques do dia (1-based), ou null. */
        Integer myOrder,
        /** Total de embarques marcados para hoje. */
        Integer totalStops,
        /** Indica se o dependente está marcado para ir hoje. */
        boolean goingToday
) {
    /** Resposta vazia quando o responsável ainda não tem transportador ativo. */
    public static GuardianTrackingResponse none() {
        return new GuardianTrackingResponse(
                false, null, null, null, null, null, null, null, null, null, false);
    }
}
