package com.vanbora.api.modules.auth.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanbora.api.shared.exception.FeatureUnavailableException;
import com.vanbora.api.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifica o {@code id_token} devolvido pelo Auth0 depois do login com Google ou
 * Facebook.
 *
 * O app manda apenas o id_token; confiar no que ele diz seria o mesmo que aceitar
 * "sou fulano@gmail.com" de qualquer cliente. Por isso a verificação acontece aqui:
 *
 *   1. assinatura RS256 conferida com a chave pública do tenant (JWKS);
 *   2. algoritmo obrigatoriamente RS256 — {@code none} e HMAC são recusados
 *      (aceitar HMAC permitiria assinar um token com a própria chave pública, que
 *      é de conhecimento geral);
 *   3. emissor igual ao do tenant configurado;
 *   4. audiência entre os client ids autorizados — impede que um id_token emitido
 *      para outro aplicativo do mesmo tenant sirva de credencial aqui;
 *   5. validade (exp/nbf), com 60s de tolerância de relógio.
 *
 * As chaves públicas ficam em cache: baixar o JWKS a cada login somaria uma ida à
 * rede no caminho crítico. Um {@code kid} desconhecido força uma nova busca (o
 * Auth0 rotaciona chaves), no máximo uma vez por minuto para não virar um vetor
 * de tráfego contra o próprio Auth0.
 */
@Component
public class Auth0TokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(Auth0TokenVerifier.class);

    private static final String REQUIRED_ALGORITHM = "RS256";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MIN_REFETCH_INTERVAL = Duration.ofMinutes(1);
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final Auth0Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile Map<String, RSAPublicKey> keysByKid = Map.of();
    private volatile Instant keysFetchedAt = Instant.EPOCH;

    public Auth0TokenVerifier(Auth0Properties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    }

    /** Valida o id_token e devolve a identidade social; lança se algo não conferir. */
    public SocialIdentity verify(String idToken) {
        if (!properties.isEnabled()) {
            throw new FeatureUnavailableException(
                    "Login com Google/Facebook não está configurado nesta instalação.");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Token do login social ausente.");
        }

        JsonNode header = decodeHeader(idToken);
        String algorithm = header.path("alg").asText();
        if (!REQUIRED_ALGORITHM.equals(algorithm)) {
            throw new UnauthorizedException("Token do login social usa um algoritmo não aceito.");
        }
        String kid = header.path("kid").asText(null);
        RSAPublicKey publicKey = resolveKey(kid);

        Claims claims = parseClaims(idToken, publicKey);
        validateAudience(claims);

        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException(
                    "A conta social não informou um e-mail. Cadastre-se com e-mail e senha.");
        }
        Boolean emailVerified = claims.get("email_verified", Boolean.class);
        String subject = claims.getSubject();

        return new SocialIdentity(
                subject,
                SocialIdentity.providerFromSubject(subject),
                email.trim().toLowerCase(),
                Boolean.TRUE.equals(emailVerified),
                claims.get("name", String.class));
    }

    private Claims parseClaims(String idToken, RSAPublicKey publicKey) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(idToken);
            return jws.getPayload();
        } catch (Exception ex) {
            throw new UnauthorizedException("Login social inválido ou expirado. Tente novamente.");
        }
    }

    /**
     * A audiência do id_token é o client id da aplicação. Sem esta checagem, um
     * token emitido para qualquer outro app do mesmo tenant seria aceito.
     */
    private void validateAudience(Claims claims) {
        List<String> expected = properties.audiences();
        if (expected.isEmpty()) {
            // Nada configurado: o próprio startup avisa; aqui só não há o que comparar.
            return;
        }
        var actual = claims.getAudience();
        if (actual == null || expected.stream().noneMatch(actual::contains)) {
            throw new UnauthorizedException("Login social não pertence a este aplicativo.");
        }
    }

    private JsonNode decodeHeader(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("formato inesperado");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new UnauthorizedException("Token do login social malformado.");
        }
    }

    private RSAPublicKey resolveKey(String kid) {
        Map<String, RSAPublicKey> cached = keysByKid;
        RSAPublicKey key = kid != null ? cached.get(kid) : null;
        boolean stale = Instant.now()
                .isAfter(keysFetchedAt.plus(Duration.ofMinutes(properties.jwksCacheMinutes())));

        if (key != null && !stale) {
            return key;
        }
        if (key == null && !canRefetch()) {
            throw new UnauthorizedException("Não foi possível validar o login social. Tente novamente.");
        }

        Map<String, RSAPublicKey> fresh = fetchKeys();
        keysByKid = fresh;
        keysFetchedAt = Instant.now();

        RSAPublicKey resolved = kid != null ? fresh.get(kid) : singleKey(fresh);
        if (resolved == null) {
            throw new UnauthorizedException("Login social assinado por uma chave desconhecida.");
        }
        return resolved;
    }

    /** Evita uma busca de JWKS por requisição quando o kid simplesmente não existe. */
    private boolean canRefetch() {
        return Instant.now().isAfter(keysFetchedAt.plus(MIN_REFETCH_INTERVAL));
    }

    private RSAPublicKey singleKey(Map<String, RSAPublicKey> keys) {
        return keys.size() == 1 ? keys.values().iterator().next() : null;
    }

    private Map<String, RSAPublicKey> fetchKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.jwksUrl()))
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("JWKS respondeu " + response.statusCode());
            }
            return parseJwks(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UnauthorizedException("Não foi possível validar o login social. Tente novamente.");
        } catch (Exception ex) {
            log.warn("Falha ao buscar as chaves públicas do Auth0 em {}: {}",
                    properties.jwksUrl(), ex.getMessage());
            throw new UnauthorizedException("Não foi possível validar o login social. Tente novamente.");
        }
    }

    private Map<String, RSAPublicKey> parseJwks(String body) throws Exception {
        JsonNode keys = objectMapper.readTree(body).path("keys");
        Map<String, RSAPublicKey> result = new HashMap<>();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        for (JsonNode jwk : keys) {
            if (!"RSA".equals(jwk.path("kty").asText())) {
                continue;
            }
            String use = jwk.path("use").asText("sig");
            if (!"sig".equals(use)) {
                continue;
            }
            String kid = jwk.path("kid").asText(null);
            String modulus = jwk.path("n").asText(null);
            String exponent = jwk.path("e").asText(null);
            if (kid == null || modulus == null || exponent == null) {
                continue;
            }
            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, Base64.getUrlDecoder().decode(modulus)),
                    new BigInteger(1, Base64.getUrlDecoder().decode(exponent)));
            result.put(kid, (RSAPublicKey) keyFactory.generatePublic(spec));
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("JWKS sem chaves RSA de assinatura");
        }
        return Map.copyOf(result);
    }
}
