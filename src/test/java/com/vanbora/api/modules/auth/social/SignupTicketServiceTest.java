package com.vanbora.api.modules.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vanbora.api.security.jwt.JwtProperties;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

class SignupTicketServiceTest {

    private static final JwtProperties JWT_PROPERTIES =
            new JwtProperties("teste-secret-teste-secret-teste-secret-0123456789", 3_600_000L);

    private final SignupTicketService service = new SignupTicketService(
            JWT_PROPERTIES, new Auth0Properties("vanbora.us.auth0.com", "abc123", 60, 30));

    private final SocialIdentity identity = new SocialIdentity(
            "google-oauth2|1234", "google", "mariana@exemplo.com", true, "Mariana");

    @Test
    void emiteEValidaOTicketDoMesmoEmail() {
        SocialIdentity verified = service.verify(service.issue(identity), "mariana@exemplo.com");

        assertThat(verified.email()).isEqualTo("mariana@exemplo.com");
        assertThat(verified.provider()).isEqualTo("google");
        assertThat(verified.subject()).isEqualTo("google-oauth2|1234");
        assertThat(verified.name()).isEqualTo("Mariana");
    }

    @Test
    void aceitaVariacaoDeCaixaEEspacosNoEmail() {
        assertThat(service.verify(service.issue(identity), "  Mariana@Exemplo.com "))
                .isNotNull();
    }

    @Test
    void recusaTicketUsadoParaOutroEmail() {
        String ticket = service.issue(identity);

        assertThatThrownBy(() -> service.verify(ticket, "outra.pessoa@exemplo.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("diferente do e-mail da conta social");
    }

    @Test
    void recusaTicketAdulterado() {
        String ticket = service.issue(identity);
        String tampered = ticket.substring(0, ticket.length() - 2) + "xy";

        assertThatThrownBy(() -> service.verify(tampered, "mariana@exemplo.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void ticketDeCadastroNaoValeComoTokenDeSessao() {
        // O ticket é assinado com uma chave derivada, não com a do JWT de sessão.
        // Se valesse como sessão, quem tem um ticket entraria na conta do e-mail
        // assim que ela passasse a existir.
        JwtService jwtService = new JwtService(JWT_PROPERTIES);

        assertThat(jwtService.isValid(service.issue(identity))).isFalse();
    }
}
