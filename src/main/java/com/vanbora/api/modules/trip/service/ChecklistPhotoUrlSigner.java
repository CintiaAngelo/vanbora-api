package com.vanbora.api.modules.trip.service;

import com.vanbora.api.security.jwt.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * URLs assinadas (HMAC-SHA256) e de curta duração para as fotos do checklist.
 *
 * <p>O componente de imagem do app (e o da web) não envia o cabeçalho Authorization, então a
 * foto não pode depender do JWT. A API só assina a URL para quem passou na checagem de acesso
 * do checklist; a URL vale por poucos minutos e é presa ao id da foto. Sem assinatura válida
 * o arquivo não sai — ao contrário de /uploads, que é público.
 */
@Component
public class ChecklistPhotoUrlSigner {

    static final Duration TTL = Duration.ofMinutes(15);
    private static final String ALGORITHM = "HmacSHA256";
    /** Separa esta chave de outros usos do segredo (o JWT usa o segredo puro). */
    private static final String CONTEXT = "vanbora:checklist-photo:";

    private final byte[] key;
    private final Clock clock;

    @Autowired
    public ChecklistPhotoUrlSigner(JwtProperties jwtProperties) {
        this(jwtProperties.secret(), Clock.systemUTC());
    }

    ChecklistPhotoUrlSigner(String secret, Clock clock) {
        this.key = (CONTEXT + secret).getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    /** URL relativa da foto (use mediaUrl() no app), válida por {@link #TTL}. */
    public String signedPath(Long photoId) {
        long exp = clock.instant().plus(TTL).getEpochSecond();
        return "/api/media/checklist-photos/%d?exp=%d&sig=%s".formatted(photoId, exp, sign(photoId, exp));
    }

    /** Assinatura confere e não expirou? Comparação em tempo constante. */
    public boolean isValid(Long photoId, long exp, String signature) {
        if (photoId == null || signature == null || clock.instant().getEpochSecond() > exp) {
            return false;
        }
        byte[] expected = sign(photoId, exp).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(Long photoId, long exp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] raw = mac.doFinal((photoId + ":" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponível", e);
        }
    }
}
