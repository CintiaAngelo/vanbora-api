package com.vanbora.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vanbora.api.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: garante que todo o contexto Spring sobe (beans, segurança,
 * mapeamentos JPA) e que o seeding de demonstração roda sem erros.
 */
@SpringBootTest
@ActiveProfiles("test")
class VanBoraApiApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoadsAndSeedsDemoData() {
        // O DataSeeder roda na inicialização do contexto.
        assertThat(userRepository.count()).isGreaterThan(0);
        assertThat(userRepository.findByEmail("roberto@vanbora.com")).isPresent();
        assertThat(userRepository.findByEmail("mariana@vanbora.com")).isPresent();
    }
}
