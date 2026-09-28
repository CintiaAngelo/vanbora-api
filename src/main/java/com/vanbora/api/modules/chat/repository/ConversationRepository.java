package com.vanbora.api.modules.chat.repository;

import com.vanbora.api.modules.chat.domain.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByGuardianUserId(Long guardianUserId);

    List<Conversation> findByTransporterUserId(Long transporterUserId);

    List<Conversation> findByTransporterId(Long transporterId);

    Optional<Conversation> findByGuardianIdAndTransporterId(Long guardianId, Long transporterId);

    /**
     * Indica se o usuário participa da conversa: responsável, transportador ou membro ATUAL
     * da equipe (monitor ainda vinculado e com a conta ativa). Usado no SUBSCRIBE e em cada
     * entrega do WebSocket — quem perde o vínculo deixa de receber mensagens novas na hora.
     */
    @Query("""
            select (count(c) > 0) from Conversation c
            where c.id = :conversationId
              and (c.guardian.user.id = :userId
                   or c.transporter.user.id = :userId
                   or exists (select m.id from ConversationMember m
                              where m.conversation = c and m.user.id = :userId
                                and m.removedAt is null
                                and (m.user.active is null or m.user.active = true)))
            """)
    boolean existsByIdAndParticipant(@Param("conversationId") Long conversationId,
                                     @Param("userId") Long userId);
}
