package com.vanbora.api.modules.notification;

import com.vanbora.api.modules.notification.domain.PushToken;
import com.vanbora.api.modules.notification.repository.PushTokenRepository;
import com.vanbora.api.modules.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Notificações push via Expo Push API (https://exp.host). Os tokens são dos
 * aparelhos (sem segredos no servidor). O envio é best-effort: qualquer falha é
 * apenas logada e nunca quebra a operação de negócio que disparou a notificação.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RestClient client;

    public PushNotificationService(
            PushTokenRepository tokenRepository,
            UserRepository userRepository,
            @Value("${vanbora.push.expo-url:https://exp.host/--/api/v2/push/send}") String expoUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.client = RestClient.builder().baseUrl(expoUrl).build();
    }

    /** Registra (ou atualiza) o token de push do aparelho para o usuário. */
    @Transactional
    public void registerToken(Long userId, String token, String platform) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        PushToken entity = tokenRepository.findByToken(token).orElseGet(PushToken::new);
        entity.setUser(userRepository.getReferenceById(userId));
        entity.setToken(token.trim());
        entity.setPlatform(platform);
        tokenRepository.save(entity);
    }

    /** Remove o token (ex.: logout). */
    @Transactional
    public void removeToken(String token) {
        if (StringUtils.hasText(token)) {
            tokenRepository.deleteByToken(token.trim());
        }
    }

    /** Envia uma notificação a todos os aparelhos do usuário (best-effort). */
    public void sendToUser(Long userId, String title, String body, Map<String, Object> data) {
        List<PushToken> tokens = tokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            return;
        }
        List<Map<String, Object>> messages = tokens.stream()
                .map(t -> buildMessage(t.getToken(), title, body, data))
                .toList();
        try {
            client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(messages)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Falha ao enviar push para o usuário {}: {}", userId, e.getMessage());
        }
    }

    private Map<String, Object> buildMessage(String to, String title, String body, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("to", to);
        message.put("title", title);
        message.put("body", body);
        message.put("sound", "default");
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }
        return message;
    }
}
