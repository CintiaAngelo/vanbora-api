package com.vanbora.api.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private static final int AUTH_LIMIT = 3;

    private final RateLimitFilter filter = new RateLimitFilter(
            new RateLimitProperties(true, AUTH_LIMIT, 50, 100, false),
            // Equivalente ao ObjectMapper que o Boot injeta em produção: o corpo do
            // erro tem um Instant, que exige o módulo de datas do Java 8.
            new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void permiteAteOLimiteEDepoisRecusaComRetryAfter() throws Exception {
        for (int attempt = 1; attempt <= AUTH_LIMIT; attempt++) {
            MockHttpServletResponse response = post("/api/auth/login", "203.0.113.10");
            assertThat(response.getStatus())
                    .as("tentativa %d deveria passar", attempt)
                    .isEqualTo(HttpServletResponse.SC_OK);
        }

        MockHttpServletResponse blocked = post("/api/auth/login", "203.0.113.10");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getContentAsString()).contains("Muitas requisições");
    }

    @Test
    void contaSeparadamentePorIp() throws Exception {
        for (int attempt = 1; attempt <= AUTH_LIMIT; attempt++) {
            post("/api/auth/login", "203.0.113.10");
        }

        // Outro IP não deve herdar o bloqueio do primeiro.
        assertThat(post("/api/auth/login", "203.0.113.99").getStatus())
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void faixaDeAutenticacaoEMaisEstreitaQueAsDemais() throws Exception {
        for (int attempt = 1; attempt <= AUTH_LIMIT; attempt++) {
            post("/api/auth/login", "203.0.113.20");
        }
        assertThat(post("/api/auth/login", "203.0.113.20").getStatus()).isEqualTo(429);

        // O mesmo IP continua podendo usar o restante da API: o limite de /api/auth
        // não pode derrubar quem já está logado usando o app.
        assertThat(post("/api/guardian/dashboard", "203.0.113.20").getStatus())
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void ignoraXForwardedForQuandoNaoHaProxyConfiavel() throws Exception {
        // Sem trust-forwarded-for, trocar o cabeçalho não deve zerar o contador —
        // senão qualquer cliente escaparia do limite forjando o próprio IP.
        for (int attempt = 1; attempt <= AUTH_LIMIT; attempt++) {
            postForwarded("198.51.100.1", "10.0.0." + attempt);
        }
        assertThat(postForwarded("198.51.100.1", "10.0.0.99").getStatus()).isEqualTo(429);
    }

    @Test
    void naoLimitaPreflightDoCors() throws Exception {
        for (int attempt = 0; attempt < AUTH_LIMIT * 3; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
            request.setRemoteAddr("203.0.113.30");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        }
    }

    private MockHttpServletResponse post(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return execute(request);
    }

    private MockHttpServletResponse postForwarded(String remoteAddr, String forwardedFor)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return execute(request);
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }
}
