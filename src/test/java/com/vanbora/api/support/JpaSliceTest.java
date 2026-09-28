package com.vanbora.api.support;

import com.vanbora.api.modules.chat.service.ChatMembershipService;
import com.vanbora.api.modules.chat.service.ChatServiceImpl;
import com.vanbora.api.modules.monitor.service.MonitorAccountService;
import com.vanbora.api.modules.monitor.service.MonitorService;
import com.vanbora.api.modules.transporter.service.TransporterServiceImpl;
import com.vanbora.api.modules.trip.service.ChecklistPhotoUrlSigner;
import com.vanbora.api.modules.trip.service.SafetyChecklistNotifier;
import com.vanbora.api.modules.trip.service.SafetyChecklistServiceImpl;
import com.vanbora.api.modules.trip.service.TripAccessService;
import com.vanbora.api.modules.trip.service.TripServiceImpl;
import com.vanbora.api.security.jwt.JwtProperties;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fatia JPA (H2) com os serviços reais das funcionalidades de monitor/percurso/checklist/chat.
 *
 * <p>Não sobe o contexto completo da aplicação (que, neste ambiente, falha ao abrir o
 * HttpClient do geocoding): só entidades, repositórios e os serviços importados abaixo.
 * Dependências de I/O externo (disco, push, WebSocket) são substituídas por mocks em cada teste.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@ActiveProfiles("test")
@Import({
        JpaSliceTest.Beans.class,
        TestFixtures.class,
        TripAccessService.class,
        ChatMembershipService.class,
        MonitorAccountService.class,
        MonitorService.class,
        TripServiceImpl.class,
        SafetyChecklistServiceImpl.class,
        SafetyChecklistNotifier.class,
        ChecklistPhotoUrlSigner.class,
        TransporterServiceImpl.class,
        ChatServiceImpl.class
})
public @interface JpaSliceTest {

    @TestConfiguration
    class Beans {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4); // rápido para testes
        }

        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties("test-secret-test-secret-test-secret-0123456789", 3_600_000);
        }
    }
}
