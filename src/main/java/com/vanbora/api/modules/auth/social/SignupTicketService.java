package com.vanbora.api.modules.auth.social;

import com.vanbora.api.security.jwt.JwtProperties;
import com.vanbora.api.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * "Ticket" de cadastro social: prova, por alguns minutos, que o dono daquele
 * e-mail acabou de se autenticar no Google/Facebook.
 *
 * Serve para o caso em que o e-mail social ainda não tem conta no VanBora. Não dá
 * para criar a conta ali mesmo — falta o perfil (responsável ou transportador),
 * CPF, endereço, CNH, placa... Então o app segue para o cadastro normal, já com
 * nome e e-mail preenchidos, e envia este ticket junto. Com ele, o cadastro
 * dispensa a criação de senha, sem que o app precise reenviar o id_token do Auth0
 * (que tem validade longa e não deveria ficar circulando entre telas).
 *
 * A chave é derivada do secret do JWT, mas NÃO é a mesma: um ticket precisa ser
 * incapaz de passar por um token de sessão no filtro de autenticação, mesmo que
 * mais tarde exista um usuário com aquele e-mail.
 */
@Service
public class SignupTicketService {

    private static final String KEY_CONTEXT = "vanbora-signup-ticket-v1";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PROVIDER = "provider";
    private static final String CLAIM_SUBJECT = "socialSubject";

    private final SecretKey key;
    private final long ttlMinutes;

    public SignupTicketService(JwtProperties jwtProperties, Auth0Properties auth0Properties) {
        this.key = deriveKey(jwtProperties.secret());
        this.ttlMinutes = auth0Properties.signupTicketMinutes();
    }

    public String issue(SocialIdentity identity) {
        Date now = new Date();
        return Jwts.builder()
                .subject(identity.email())
                .claim(CLAIM_NAME, identity.name())
                .claim(CLAIM_PROVIDER, identity.provider())
                .claim(CLAIM_SUBJECT, identity.subject())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    /**
     * Valida o ticket e confere que ele pertence ao e-mail sendo cadastrado —
     * sem essa comparação, um ticket legítimo criaria conta para outro e-mail.
     */
    public SocialIdentity verify(String ticket, String email) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(ticket)
                    .getPayload();
        } catch (Exception ex) {
            throw new UnauthorizedException(
                    "O login social expirou. Entre de novo com Google ou Facebook.");
        }
        if (email == null || !email.trim().equalsIgnoreCase(claims.getSubject())) {
            throw new UnauthorizedException(
                    "O e-mail do cadastro é diferente do e-mail da conta social.");
        }
        return new SocialIdentity(
                claims.get(CLAIM_SUBJECT, String.class),
                claims.get(CLAIM_PROVIDER, String.class),
                claims.getSubject(),
                true,
                claims.get(CLAIM_NAME, String.class));
    }

    /** HMAC do secret do JWT com um rótulo fixo: chave distinta, mesma origem de confiança. */
    private static SecretKey deriveKey(String jwtSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return new SecretKeySpec(
                    mac.doFinal(KEY_CONTEXT.getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível derivar a chave do ticket de cadastro", ex);
        }
    }
}
