package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.modules.notice.domain.Notice;
import java.time.Instant;

/** Aviso enviado, exibido na lista de avisos. */
public record NoticeResponse(Long id, String message, Instant createdAt, int recipients) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getMessage(),
                notice.getCreatedAt(),
                notice.getRecipientsCount());
    }
}
