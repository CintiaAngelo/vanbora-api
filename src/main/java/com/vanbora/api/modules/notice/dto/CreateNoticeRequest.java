package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.shared.enums.NoticePriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Criação de aviso: título, agendamento, prioridade, comentários e destinatários. */
public record CreateNoticeRequest(
        @Size(max = 120) String title,
        @NotBlank @Size(max = 600) String message,
        /** Horas até publicar (null ou 0 = agora). Máx. 1 semana. */
        @Min(0) @Max(168) Integer publishInHours,
        /** null = aceita comentários (padrão). */
        Boolean allowComments,
        /** null = INFO. */
        NoticePriority priority,
        /** null/vazio = todos os responsáveis; senão, apenas estes (ids de GuardianProfile). */
        List<Long> recipientGuardianIds
) {
}
