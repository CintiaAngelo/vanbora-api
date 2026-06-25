package com.vanbora.api.modules.notice.dto;

import com.vanbora.api.modules.notice.domain.NoticeReaction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reações agrupadas por emoji, com os nomes de quem reagiu (visível no mural). */
public record ReactionGroup(String emoji, int count, List<String> names) {

    public static List<ReactionGroup> from(List<NoticeReaction> reactions) {
        Map<String, List<String>> byEmoji = new LinkedHashMap<>();
        for (NoticeReaction reaction : reactions) {
            byEmoji.computeIfAbsent(reaction.getEmoji(), k -> new ArrayList<>())
                    .add(reaction.getUser().getName());
        }
        return byEmoji.entrySet().stream()
                .map(e -> new ReactionGroup(e.getKey(), e.getValue().size(), e.getValue()))
                .toList();
    }
}
