package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.modules.notice.domain.NoticeComment;
import java.time.Instant;

/** Comentário exibido no aviso. `mine` indica se é do próprio usuário (para excluir). */
public record NoticeCommentResponse(
        Long id,
        String authorName,
        String text,
        Instant createdAt,
        boolean mine
) {
    public static NoticeCommentResponse from(NoticeComment comment, Long currentUserId) {
        return new NoticeCommentResponse(
                comment.getId(),
                comment.getAuthor().getName(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getAuthor().getId().equals(currentUserId));
    }
}
