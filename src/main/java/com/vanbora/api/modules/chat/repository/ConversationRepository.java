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

    Optional<Conversation> findByGuardianIdAndTransporterId(Long guardianId, Long transporterId);

    /** Indica se o usuário participa (responsável ou transportador) da conversa. */
    @Query("""
            select (count(c) > 0) from Conversation c
            where c.id = :conversationId
              and (c.guardian.user.id = :userId or c.transporter.user.id = :userId)
            """)
    boolean existsByIdAndParticipant(@Param("conversationId") Long conversationId,
                                     @Param("userId") Long userId);
}
