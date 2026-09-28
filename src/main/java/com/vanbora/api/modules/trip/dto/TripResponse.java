package com.vanbora.api.modules.trip.dto;

import java.util.List;

/**
 * Percurso com o estado da execução mais recente.
 *
 * @param studentIds dependentes escolhidos (só na visão do transportador, para edição;
 *                   nulo para o monitor)
 * @param currentRun execução mais recente (em andamento ou já encerrada hoje); nula se nunca rodou
 */
public record TripResponse(
        Long id,
        String name,
        boolean allStudents,
        int studentCount,
        List<TripMonitorResponse> monitors,
        List<Long> studentIds,
        TripRunResponse currentRun
) {

    /** Monitor escalado no percurso. */
    public record TripMonitorResponse(Long helperId, String name, String photoUrl, boolean accessActive) {
    }
}
