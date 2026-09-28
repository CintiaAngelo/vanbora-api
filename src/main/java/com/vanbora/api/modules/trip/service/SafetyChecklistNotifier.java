package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.SafetyChecklistRepository;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notificações do checklist, pela infraestrutura de push já existente
 * ({@link PushNotificationService}, best-effort). Disparadas só após o commit.
 */
@Component
public class SafetyChecklistNotifier {

    private static final Logger log = LoggerFactory.getLogger(SafetyChecklistNotifier.class);

    private final SafetyChecklistRepository checklistRepository;
    private final TripAccessService access;
    private final PushNotificationService pushNotificationService;

    public SafetyChecklistNotifier(SafetyChecklistRepository checklistRepository,
                                   TripAccessService access,
                                   PushNotificationService pushNotificationService) {
        this.checklistRepository = checklistRepository;
        this.access = access;
        this.pushNotificationService = pushNotificationService;
    }

    /** Percurso encerrado: avisa motorista e monitores escalados que há verificação pendente. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onChecklistOpened(TripEvents.ChecklistOpened event) {
        SafetyChecklist checklist = checklistRepository.findById(event.checklistId()).orElse(null);
        if (checklist == null) {
            return;
        }
        Trip trip = checklist.getRun().getTrip();
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(trip.getTransporter().getUser().getId());
        for (Helper helper : trip.getMonitors()) {
            if (helper.hasActiveAccess()) {
                recipients.add(helper.getUser().getId());
            }
        }
        Map<String, Object> data = Map.of("type", "SAFETY_CHECK_PENDING", "checklistId", checklist.getId());
        for (Long userId : recipients) {
            pushNotificationService.sendToUser(userId,
                    "⚠️ Verificação de segurança pendente",
                    "O percurso " + trip.getName() + " terminou. Confira o interior da van e registre a foto.",
                    data);
        }
    }

    /** Checklist concluído: avisa todos os responsáveis dos alunos daquele percurso. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onChecklistCompleted(TripEvents.ChecklistCompleted event) {
        SafetyChecklist checklist = checklistRepository.findById(event.checklistId()).orElse(null);
        if (checklist == null) {
            return;
        }
        Trip trip = checklist.getRun().getTrip();
        Set<Long> guardianUserIds = new LinkedHashSet<>();
        for (Enrollment enrollment : access.activeEnrollmentsOf(trip)) {
            guardianUserIds.add(enrollment.getDependent().getGuardian().getUser().getId());
        }
        Map<String, Object> data = Map.of("type", "SAFETY_CHECK_COMPLETED", "checklistId", checklist.getId());
        for (Long userId : guardianUserIds) {
            pushNotificationService.sendToUser(userId,
                    "✅ Verificação de segurança concluída",
                    "A conferência interna da van foi realizada após o término do percurso "
                            + trip.getName() + " e o veículo foi verificado.",
                    data);
        }
        checklist.setGuardiansNotified(guardianUserIds.size());
        checklistRepository.save(checklist);
        log.info("Checklist {} concluído: {} responsável(is) notificado(s).", checklist.getId(), guardianUserIds.size());
    }
}
