package com.vanbora.api.security;

import com.vanbora.api.modules.user.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acesso ao usuário autenticado no contexto de segurança.
 * Encapsula o SecurityContextHolder para que os serviços não dependam dele diretamente.
 */
@Component
public class CurrentUserProvider {

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto.");
        }
        return details.getUser();
    }

    public Long requireUserId() {
        return requireUser().getId();
    }
}
