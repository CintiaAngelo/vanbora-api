package com.vanbora.api;

import com.vanbora.api.shared.fee.FeeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ponto de entrada da API VanBora. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(FeeProperties.class)
public class VanBoraApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VanBoraApiApplication.class, args);
    }
}
