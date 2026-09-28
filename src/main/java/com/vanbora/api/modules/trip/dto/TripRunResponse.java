package com.vanbora.api.modules.trip.dto;

import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.trip.domain.TripRun;
import com.vanbora.api.shared.enums.ChecklistStatus;
import com.vanbora.api.shared.enums.TripRunStatus;
import java.time.Instant;
import java.time.LocalDate;

/** Execução de um percurso + o checklist gerado ao encerrá-la (quando houver). */
public record TripRunResponse(
        Long id,
        Long tripId,
        TripRunStatus status,
        LocalDate serviceDate,
        Instant startedAt,
        Instant finishedAt,
        String startedByName,
        String finishedByName,
        Long checklistId,
        ChecklistStatus checklistStatus
) {
    public static TripRunResponse from(TripRun run, SafetyChecklist checklist) {
        return new TripRunResponse(
                run.getId(),
                run.getTrip().getId(),
                run.getStatus(),
                run.getServiceDate(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStartedBy() != null ? run.getStartedBy().getName() : null,
                run.getFinishedBy() != null ? run.getFinishedBy().getName() : null,
                checklist != null ? checklist.getId() : null,
                checklist != null ? checklist.getStatus() : null);
    }
}
