package com.vanbora.api.modules.trip.dto;

import com.vanbora.api.shared.enums.ChecklistStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Checklist de segurança pós-percurso, igual para motorista e monitor. Quando concluído,
 * traz quem realizou, com que perfil e quando — o outro profissional vê que a verificação
 * já foi feita em vez de o cartão simplesmente sumir.
 *
 * @param completedByMe o usuário autenticado foi quem concluiu
 * @param photosPurgeAt data prevista para a exclusão automática das fotos
 * @param photosPurgedAt quando as fotos foram excluídas (histórico permanece)
 */
public record SafetyChecklistResponse(
        Long id,
        Long runId,
        Long tripId,
        String tripName,
        LocalDate serviceDate,
        Instant tripFinishedAt,
        ChecklistStatus status,
        String completedByName,
        String completedByRole,
        Instant completedAt,
        boolean completedByMe,
        List<ChecklistItemResponse> items,
        List<ChecklistPhotoResponse> photos,
        int photoCount,
        Instant photosPurgeAt,
        Instant photosPurgedAt,
        Integer guardiansNotified
) {

    /** Item do checklist com o rótulo exibido e se foi confirmado na conclusão. */
    public record ChecklistItemResponse(String code, String label, boolean confirmed) {
    }

    /**
     * Foto do checklist. {@code url} é uma URL assinada e de curta duração (nula quando a
     * foto já expirou pela retenção).
     */
    public record ChecklistPhotoResponse(Long id, String url, boolean available, Instant uploadedAt) {
    }
}
