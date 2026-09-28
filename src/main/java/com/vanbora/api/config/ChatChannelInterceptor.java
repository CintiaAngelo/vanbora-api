package com.vanbora.api.config;

import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.security.AppUserDetails;
import com.vanbora.api.security.AppUserDetailsService;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.security.jwt.TokenRevocationService;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Segurança do canal STOMP:
 *  - CONNECT: valida o JWT (header Authorization) e fixa o usuário na sessão WS.
 *  - SUBSCRIBE: garante que o usuário participa da conversa do tópico assinado,
 *    impedindo que alguém escute conversas alheias.
 */
@Component
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final String TOPIC_PREFIX = "/topic/conversations/";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;
    private final TokenRevocationService tokenRevocationService;
    /** Sessão STOMP → usuário autenticado no CONNECT (para checar as entregas de saída). */
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    public ChatChannelInterceptor(JwtService jwtService,
                                  AppUserDetailsService userDetailsService,
                                  ConversationRepository conversationRepository,
                                  TokenRevocationService tokenRevocationService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.conversationRepository = conversationRepository;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())
                && accessor.getSessionId() != null) {
            sessionUsers.remove(accessor.getSessionId());
        }
        return message;
    }

    /**
     * Canal de SAÍDA: reconfere a participação a cada entrega de mensagem de conversa. Sem
     * isto, um monitor removido/desativado que já estava inscrito continuaria recebendo as
     * mensagens novas pelo WebSocket aberto até reconectar.
     */
    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                // Na saída o broker ainda não converteu para STOMP: os cabeçalhos são os
                // genéricos do SIMP (tipo MESSAGE, destino e sessão do assinante).
                var headers = message.getHeaders();
                if (SimpMessageHeaderAccessor.getMessageType(headers) != SimpMessageType.MESSAGE) {
                    return message;
                }
                String destination = SimpMessageHeaderAccessor.getDestination(headers);
                if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
                    return message;
                }
                String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
                Long userId = sessionId != null ? sessionUsers.get(sessionId) : null;
                Long conversationId = parseConversationId(destination);
                if (userId == null || conversationId == null
                        || !conversationRepository.existsByIdAndParticipant(conversationId, userId)) {
                    return null; // descarta a entrega para esta sessão
                }
                return message;
            }
        };
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = bearer(accessor.getFirstNativeHeader("Authorization"));
        if (token == null || !jwtService.isValid(token)
                || tokenRevocationService.isRevoked(jwtService.extractTokenId(token))) {
            throw new MessagingException("Token de autenticação ausente ou inválido.");
        }
        AppUserDetails details =
                (AppUserDetails) userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
        if (!details.isEnabled()) {
            throw new MessagingException("Este acesso foi desativado.");
        }
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        accessor.setUser(auth);
        if (accessor.getSessionId() != null) {
            sessionUsers.put(accessor.getSessionId(), details.getId());
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            return; // outros tópicos não exigem checagem de conversa
        }
        Long conversationId = parseConversationId(destination);
        Long userId = currentUserId(accessor.getUser());
        if (conversationId == null || userId == null
                || !conversationRepository.existsByIdAndParticipant(conversationId, userId)) {
            throw new MessagingException("Você não tem acesso a esta conversa.");
        }
    }

    private Long currentUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof AppUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private Long parseConversationId(String destination) {
        try {
            return Long.valueOf(destination.substring(TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String bearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return header; // permite enviar o token puro também
    }
}
