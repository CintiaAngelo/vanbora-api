package com.vanbora.api.modules.notification.domain;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Token de push (Expo) de um aparelho do usuário, usado para notificá-lo. */
@Entity
@Table(name = "push_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PushToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Token Expo do aparelho (ExponentPushToken[...]). Único por dispositivo. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(length = 16)
    private String platform;
}
