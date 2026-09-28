package com.vanbora.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.security.AppUserDetails;
import com.vanbora.api.security.AppUserDetailsService;
import com.vanbora.api.security.jwt.JwtProperties;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.security.jwt.TokenRevocationService;
import com.vanbora.api.shared.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * WebSocket do chat: conta desativada não conecta, e quem perde o vínculo com a conversa
 * deixa de receber as entregas mesmo com a assinatura antiga ainda aberta.
 */
class ChatChannelInterceptorTest {

    private final JwtService jwtService =
            new JwtService(new JwtProperties("test-secret-test-secret-test-secret-0123456789-abc", 3_600_000));
    private final AppUserDetailsService userDetailsService = mock(AppUserDetailsService.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final TokenRevocationService revocationService = mock(TokenRevocationService.class);
    private final ChatChannelInterceptor interceptor = new ChatChannelInterceptor(
            jwtService, userDetailsService, conversationRepository, revocationService);
    private final MessageChannel channel = mock(MessageChannel.class);

    private User monitor(boolean active) {
        User user = new User();
        user.setId(7L);
        user.setName("João");
        user.setEmail("joao@exemplo.com");
        user.setPasswordHash("x");
        user.setPhone("11999990000");
        user.setRole(UserRole.MONITOR);
        user.setActive(active);
        return user;
    }

    private Message<byte[]> connect(User user) {
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(new AppUserDetails(user));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("s1");
        accessor.addNativeHeader("Authorization", "Bearer " + jwtService.generateToken(user));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> delivery(String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId("s1");
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void contaDesativadaNaoConecta() {
        Message<byte[]> connect = connect(monitor(false));
        assertThatThrownBy(() -> interceptor.preSend(connect, channel)).isInstanceOf(MessagingException.class);
    }

    @Test
    void entregaSoParaQuemAindaParticipaDaConversa() {
        interceptor.preSend(connect(monitor(true)), channel);
        when(conversationRepository.existsByIdAndParticipant(10L, 7L)).thenReturn(true);

        assertThat(interceptor.outbound().preSend(delivery("/topic/conversations/10"), channel)).isNotNull();

        // Monitor removido do percurso: a próxima mensagem não chega a esta sessão.
        when(conversationRepository.existsByIdAndParticipant(10L, 7L)).thenReturn(false);
        assertThat(interceptor.outbound().preSend(delivery("/topic/conversations/10"), channel)).isNull();
    }

    @Test
    void entregaParaSessaoDesconhecidaEDescartada() {
        assertThat(interceptor.outbound().preSend(delivery("/topic/conversations/10"), channel)).isNull();
    }
}
