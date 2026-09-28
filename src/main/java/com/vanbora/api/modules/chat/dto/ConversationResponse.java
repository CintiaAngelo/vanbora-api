package com.vanbora.api.modules.chat.dto;

import java.util.List;

/** Resumo de conversa para a lista de chats. */
public record ConversationResponse(
        Long id,
        String name,
        String lastMessage,
        String time,
        int unread,
        /** Id do perfil do transportador da conversa (para abrir o perfil pelo chat). */
        Long transporterId,
        /** Há monitores no grupo (conversa após a contratação). */
        boolean group,
        /** Quem participa hoje: responsável, transportador e monitores vinculados. */
        List<ParticipantResponse> participants
) {

    /** Participante exibido no cabeçalho do grupo. */
    public record ParticipantResponse(Long userId, String name, String role) {
    }
}
