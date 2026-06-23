package com.vanbora.api.modules.chat.repository;

import com.vanbora.api.modules.chat.domain.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
}
