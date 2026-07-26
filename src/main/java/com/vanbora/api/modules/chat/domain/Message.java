package com.vanbora.api.modules.chat.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import com.vanbora.api.shared.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mensagem trocada dentro de uma conversa. */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String text;

    /** Tipo do conteúdo. Nullable (ddl-auto em tabela populada); null ⇒ TEXT. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MessageType type = MessageType.TEXT;

    /** URL pública da mídia (quando type=IMAGE). */
    @Column(name = "media_url", length = 512)
    private String mediaUrl;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /** Considera nulo (registros antigos) como TEXT. */
    public MessageType typeOrDefault() {
        return type == null ? MessageType.TEXT : type;
    }
}
