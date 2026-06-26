package com.vanbora.api.modules.hire.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Varre periodicamente as solicitações de contratação pendentes e expira as que
 * passaram do prazo (3 dias) sem o transportador aceitar ou recusar.
 */
@Component
public class HireRequestExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(HireRequestExpiryJob.class);

    private final HireService hireService;

    public HireRequestExpiryJob(HireService hireService) {
        this.hireService = hireService;
    }

    /** A cada 30 min (após 1 min do start). O prazo é de dias, então a folga é irrelevante. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 1_800_000)
    public void run() {
        int expired = hireService.expireOverdue();
        if (expired > 0) {
            log.info("Solicitações de contratação expiradas automaticamente: {}", expired);
        }
    }
}
