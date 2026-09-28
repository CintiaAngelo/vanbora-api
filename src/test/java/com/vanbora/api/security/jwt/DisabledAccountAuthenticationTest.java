package com.vanbora.api.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.security.AppUserDetails;
import com.vanbora.api.security.AppUserDetailsService;
import com.vanbora.api.shared.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Revogação imediata: um token ainda válido de um monitor desativado/removido deixa de
 * autenticar na requisição seguinte (o usuário é recarregado do banco a cada chamada).
 */
class DisabledAccountAuthenticationTest {

    private final JwtService jwtService =
            new JwtService(new JwtProperties("test-secret-test-secret-test-secret-0123456789-abc", 3_600_000));
    private final AppUserDetailsService userDetailsService = mock(AppUserDetailsService.class);
    private final TokenRevocationService revocationService = mock(TokenRevocationService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtService, userDetailsService, revocationService);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User monitor(boolean active) {
        User user = new User();
        user.setId(42L);
        user.setName("João");
        user.setEmail("joao@exemplo.com");
        user.setPasswordHash("x");
        user.setPhone("11999990000");
        user.setRole(UserRole.MONITOR);
        user.setActive(active);
        return user;
    }

    private void callWithToken(User issuedFor, User currentState) throws Exception {
        String token = jwtService.generateToken(issuedFor);
        when(userDetailsService.loadUserByUsername("joao@exemplo.com")).thenReturn(new AppUserDetails(currentState));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/monitors/me");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void contaAtivaAutentica() throws Exception {
        callWithToken(monitor(true), monitor(true));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_MONITOR");
    }

    @Test
    void tokenValidoDeContaDesativadaNaoAutentica() throws Exception {
        callWithToken(monitor(true), monitor(false)); // desativado depois de emitir o token
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void contaComColunaNulaContinuaAtiva() {
        User legacy = monitor(true);
        legacy.setActive(null); // registros antigos, antes da coluna existir
        assertThat(new AppUserDetails(legacy).isEnabled()).isTrue();
    }
}
