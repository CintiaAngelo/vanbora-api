package com.vanbora.api.modules.chat.repository;

import com.vanbora.api.modules.chat.domain.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByConversationId(Long conversationId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    /** Participações ATUAIS do usuário (conversas que ele ainda pode abrir). */
    @Query("""
            select m from ConversationMember m
            where m.user.id = :userId and m.removedAt is null
            order by m.conversation.id asc
            """)
    List<ConversationMember> findCurrentByUser(@Param("userId") Long userId);

    /** Membros atuais de uma conversa (exibidos no cabeçalho do grupo). */
    @Query("""
            select m from ConversationMember m
            where m.conversation.id = :conversationId and m.removedAt is null
            order by m.joinedAt asc
            """)
    List<ConversationMember> findCurrentByConversation(@Param("conversationId") Long conversationId);
}
