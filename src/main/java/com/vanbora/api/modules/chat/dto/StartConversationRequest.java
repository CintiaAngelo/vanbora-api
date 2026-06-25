package com.vanbora.api.modules.chat.dto;

import jakarta.validation.constraints.NotNull;

/** Abre (ou reusa) a conversa do responsável com um transportador. */
public record StartConversationRequest(@NotNull Long transporterId) {
}
