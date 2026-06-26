package com.vanbora.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ponto de entrada da API VanBora. */
@SpringBootApplication
@EnableScheduling
public class VanBoraApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VanBoraApiApplication.class, args);
    }
}
