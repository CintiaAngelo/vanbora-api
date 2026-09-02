package com.vanbora.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanbora.api.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Resposta para requisição sem autenticação válida: **401**, não 403.
 *
 * Sem um entry point próprio, o Spring Security devolvia 403 para token ausente,
 * expirado ou revogado — o mesmo código de "está logado, mas não pode acessar
 * isto". O app não conseguia distinguir os dois casos e, quando o token vencia,
 * o usuário ficava preso numa sessão morta vendo erros genéricos em cada tela.
 *
 * Com o 401, o app sabe que a sessão acabou e leva para o login (ver `apiFetch`).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Sessão expirada ou inválida. Entre novamente.",
                request.getRequestURI(),
                null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
