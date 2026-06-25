package com.vanbora.api.config;

import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.security.AppUserDetails;
import com.vanbora.api.security.AppUserDetailsService;
import com.vanbora.api.security.jwt.JwtService;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
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

    public ChatChannelInterceptor(JwtService jwtService,
                                  AppUserDetailsService userDetailsService,
                                  ConversationRepository conversationRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.conversationRepository = conversationRepository;
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
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = bearer(accessor.getFirstNativeHeader("Authorization"));
        if (token == null || !jwtService.isValid(token)) {
            throw new MessagingException("Token de autenticação ausente ou inválido.");
        }
        AppUserDetails details =
                (AppUserDetails) userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        accessor.setUser(auth);
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
