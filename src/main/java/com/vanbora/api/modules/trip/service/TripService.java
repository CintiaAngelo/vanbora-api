package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.trip.dto.TripRequest;
import com.vanbora.api.modules.trip.dto.TripResponse;
import com.vanbora.api.modules.trip.dto.TripRunResponse;
import com.vanbora.api.modules.user.domain.User;
import java.util.List;

/** Casos de uso dos percursos e das suas execuções (iniciar/encerrar/cancelar). */
public interface TripService {

    /** Percursos visíveis ao profissional (transportador: todos; monitor: os escalados). */
    List<TripResponse> list(User user);

    /** [Transportador] Cria um percurso com monitores e alunos. */
    TripResponse create(User transporterUser, TripRequest request);

    /** [Transportador] Edita nome, escala de monitores e alunos do percurso. */
    TripResponse update(User transporterUser, Long tripId, TripRequest request);

    /** [Transportador] Desativa o percurso (execuções e checklists antigos permanecem). */
    void delete(User transporterUser, Long tripId);

    /** Inicia a execução de hoje (no máximo uma em andamento por percurso). */
    TripRunResponse start(User user, Long tripId);

    /** Encerra a execução e abre o checklist de segurança pós-percurso. */
    TripRunResponse finish(User user, Long runId);

    /** Cancela uma execução em andamento (percurso que não ocorreu): não gera checklist. */
    TripRunResponse cancel(User user, Long runId);
}
