package com.vanbora.api.modules.chat.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profissional da equipe (monitor) incluído na conversa responsável ↔ transportador depois
 * que a contratação é concluída. O responsável e o transportador continuam sendo as duas
 * pontas fixas da {@link Conversation}; esta tabela só guarda os participantes extras.
 *
 * <p>Nunca é apagada: ao perder o vínculo o membro ganha {@code removedAt} (deixa de ler e
 * receber mensagens novas), mas as mensagens que enviou continuam no histórico. O monitor só
 * enxerga mensagens a partir de {@code joinedAt} — a negociação anterior à contratação (valores,
 * propostas) permanece exclusiva de responsável e transportador.
 */
@Entity
@Table(name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /** Quando perdeu o vínculo (nulo = participa). */
    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    public boolean isCurrent() {
        return removedAt == null;
    }
}
