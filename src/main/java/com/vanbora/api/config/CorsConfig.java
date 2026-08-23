package com.vanbora.api.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Libera o acesso dos apps (Expo) à API. As origens permitidas vêm de configuração
 * ({@code vanbora.cors.allowed-origins}), permitindo restringir em produção sem
 * alterar código. O default cobre apenas os padrões de desenvolvimento local/LAN do
 * Expo — nunca um curinga irrestrito.
 *
 * {@code allowCredentials} fica desligado: a API é stateless (Bearer JWT no header
 * Authorization), nunca usa cookies, então não há necessidade de habilitar
 * credentials — e combinar isso com origens amplas seria uma brecha desnecessária.
 */
@Configuration
public class CorsConfig {

    @Value("${vanbora.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*,exp://*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
