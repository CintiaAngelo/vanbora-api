package com.vanbora.api.modules.trip.service;

/**
 * Eventos de domínio do percurso, publicados dentro da transação e tratados só DEPOIS do
 * commit ({@code @TransactionalEventListener}): uma notificação nunca sai para algo que
 * acabou revertido (ex.: conclusão perdida para um envio simultâneo).
 */
public final class TripEvents {

    private TripEvents() {
    }

    /** Percurso encerrado: há um checklist de segurança pendente para a equipe. */
    public record ChecklistOpened(Long checklistId) {
    }

    /** Checklist concluído: os responsáveis dos alunos do percurso devem ser avisados. */
    public record ChecklistCompleted(Long checklistId) {
    }
}
