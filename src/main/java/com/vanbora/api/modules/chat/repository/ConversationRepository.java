package com.vanbora.api.modules.chat.repository;

import com.vanbora.api.modules.chat.domain.Conversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByGuardianUserId(Long guardianUserId);

    List<Conversation> findByTransporterUserId(Long transporterUserId);
}
