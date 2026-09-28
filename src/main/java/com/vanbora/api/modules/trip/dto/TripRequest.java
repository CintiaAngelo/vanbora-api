package com.vanbora.api.modules.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Cadastro/edição de um percurso.
 *
 * @param monitorIds ids dos AJUDANTES (com conta de monitor) escalados; vazio = sem monitor
 * @param allStudents true/null = leva todos os alunos ativos (inclusive futuros)
 * @param studentIds ids dos DEPENDENTES quando {@code allStudents} é falso
 */
public record TripRequest(
        @NotBlank @Size(max = 80) String name,
        List<Long> monitorIds,
        Boolean allStudents,
        List<Long> studentIds
) {
}
