package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.shared.enums.NoticePriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Edição de aviso. `publishInHours` reagenda a partir de agora (null mantém).
 * `recipientGuardianIds` null mantém o público atual; vazio = todos; lista = só estes.
 */
public record UpdateNoticeRequest(
        @Size(max = 120) String title,
        @NotBlank @Size(max = 600) String message,
        @Min(0) @Max(168) Integer publishInHours,
        Boolean allowComments,
        NoticePriority priority,
        List<Long> recipientGuardianIds
) {
}
