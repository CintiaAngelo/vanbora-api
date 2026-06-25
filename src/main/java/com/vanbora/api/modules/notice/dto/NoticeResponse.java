package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.domain.NoticeReaction;
import com.vanbora.api.shared.enums.NoticePriority;
import java.time.Instant;
import java.util.List;

/** Aviso exibido ao transportador, com agendamento, reações (quem reagiu) e engajamento. */
public record NoticeResponse(
        Long id,
        String title,
        String message,
        Instant createdAt,
        Instant publishAt,
        boolean scheduled,
        NoticePriority priority,
        boolean allowComments,
        boolean audienceAll,
        int recipients,
        int viewedCount,
        long commentsCount,
        List<ReactionGroup> reactions
) {
    public static NoticeResponse from(Notice notice, List<NoticeReaction> reactions,
                                      Instant now, int viewedCount, long commentsCount) {
        boolean scheduled = notice.getPublishAt() != null && notice.getPublishAt().isAfter(now);
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getMessage(),
                notice.getCreatedAt(),
                notice.getPublishAt(),
                scheduled,
                notice.resolvedPriority(),
                notice.isAllowComments(),
                notice.isAudienceAll(),
                notice.getRecipientsCount(),
                viewedCount,
                commentsCount,
                ReactionGroup.from(reactions));
    }
}
