package com.vanbora.api.modules.trip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retenção das fotos do checklist: a cada hora remove os ARQUIVOS cujo prazo (15 dias, ver
 * {@code vanbora.checklist.photo-retention-days}) venceu. O checklist, quem o realizou, a
 * data/hora e o registro de cada foto permanecem. Idempotente: só pega fotos ainda não
 * removidas, e arquivo já inexistente conta como removido.
 */
@Component
public class ChecklistPhotoRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ChecklistPhotoRetentionJob.class);

    private final SafetyChecklistService checklistService;

    public ChecklistPhotoRetentionJob(SafetyChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 3_600_000)
    public void run() {
        try {
            int purged = checklistService.purgeExpiredPhotos();
            if (purged > 0) {
                log.info("Fotos de checklist removidas pela retenção: {}", purged);
            }
        } catch (Exception e) {
            // Nunca derruba o agendador: a próxima rodada tenta de novo.
            log.warn("Falha na retenção das fotos de checklist: {}", e.getMessage());
        }
    }
}
