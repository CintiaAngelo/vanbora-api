package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.domain.NoticeReaction;
import com.vanbora.api.shared.enums.NoticePriority;
import java.time.Instant;
import java.util.List;

/** Aviso exibido ao responsável, com a reação dele, quem reagiu e estado de comentários. */
public record GuardianNoticeResponse(
        Long id,
        String title,
        String message,
        Instant createdAt,
        Instant publishAt,
        String transporterName,
        NoticePriority priority,
        boolean allowComments,
        String myReaction,
        long commentsCount,
        List<ReactionGroup> reactions
) {
    public static GuardianNoticeResponse from(Notice notice, List<NoticeReaction> reactions,
                                              Long userId, long commentsCount) {
        String mine = reactions.stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .map(NoticeReaction::getEmoji)
                .findFirst()
                .orElse(null);
        return new GuardianNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getMessage(),
                notice.getCreatedAt(),
                notice.getPublishAt(),
                notice.getTransporter().getUser().getName(),
                notice.resolvedPriority(),
                notice.isAllowComments(),
                mine,
                commentsCount,
                ReactionGroup.from(reactions));
    }
}
