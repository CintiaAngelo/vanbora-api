package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.trip.domain.ChecklistItem;
import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.trip.domain.SafetyChecklistPhoto;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.dto.GuardianSafetyCheckResponse;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse.ChecklistItemResponse;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse.ChecklistPhotoResponse;
import com.vanbora.api.modules.trip.repository.SafetyChecklistPhotoRepository;
import com.vanbora.api.modules.trip.repository.SafetyChecklistRepository;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.trip.service.TripAccessService.Professional;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.ChecklistStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.storage.StorageService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Implementação do checklist de segurança pós-percurso. */
@Service
public class SafetyChecklistServiceImpl implements SafetyChecklistService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.systemDefault());
    /** Janela das conferências mostradas ao responsável. */
    private static final Duration GUARDIAN_WINDOW = Duration.ofDays(7);
    private static final int GUARDIAN_LIMIT = 5;

    private final SafetyChecklistRepository checklistRepository;
    private final SafetyChecklistPhotoRepository photoRepository;
    private final TripRepository tripRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GuardianProfileRepository guardianRepository;
    private final DependentRepository dependentRepository;
    private final TripAccessService access;
    private final StorageService storageService;
    private final ChecklistPhotoUrlSigner urlSigner;
    private final ApplicationEventPublisher events;
    private final int retentionDays;
    private final int maxPhotos;
    private final Clock clock;

    @Autowired
    public SafetyChecklistServiceImpl(SafetyChecklistRepository checklistRepository,
                                      SafetyChecklistPhotoRepository photoRepository,
                                      TripRepository tripRepository,
                                      EnrollmentRepository enrollmentRepository,
                                      GuardianProfileRepository guardianRepository,
                                      DependentRepository dependentRepository,
                                      TripAccessService access,
                                      StorageService storageService,
                                      ChecklistPhotoUrlSigner urlSigner,
                                      ApplicationEventPublisher events,
                                      @Value("${vanbora.checklist.photo-retention-days:15}") int retentionDays,
                                      @Value("${vanbora.checklist.max-photos:5}") int maxPhotos) {
        this(checklistRepository, photoRepository, tripRepository, enrollmentRepository, guardianRepository,
                dependentRepository, access, storageService, urlSigner, events, retentionDays, maxPhotos,
                Clock.systemUTC());
    }

    SafetyChecklistServiceImpl(SafetyChecklistRepository checklistRepository,
                               SafetyChecklistPhotoRepository photoRepository,
                               TripRepository tripRepository,
                               EnrollmentRepository enrollmentRepository,
                               GuardianProfileRepository guardianRepository,
                               DependentRepository dependentRepository,
                               TripAccessService access,
                               StorageService storageService,
                               ChecklistPhotoUrlSigner urlSigner,
                               ApplicationEventPublisher events,
                               int retentionDays,
                               int maxPhotos,
                               Clock clock) {
        this.checklistRepository = checklistRepository;
        this.photoRepository = photoRepository;
        this.tripRepository = tripRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.guardianRepository = guardianRepository;
        this.dependentRepository = dependentRepository;
        this.access = access;
        this.storageService = storageService;
        this.urlSigner = urlSigner;
        this.events = events;
        this.retentionDays = retentionDays;
        this.maxPhotos = maxPhotos;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SafetyChecklistResponse> list(User user, ChecklistStatus status) {
        Professional professional = access.requireProfessional(user);
        List<SafetyChecklist> checklists;
        if (professional.isTransporter()) {
            checklists = checklistRepository.findForTransporter(professional.transporter().getId(), status);
        } else {
            List<Long> tripIds = access.tripsOf(professional).stream().map(Trip::getId).toList();
            checklists = tripIds.isEmpty() ? List.of() : checklistRepository.findForTrips(tripIds, status);
        }
        return checklists.stream().map(c -> toResponse(c, user, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SafetyChecklistResponse get(User user, Long checklistId) {
        Professional professional = access.requireProfessional(user);
        return toResponse(requireAccessible(professional, checklistId), user, true);
    }

    @Override
    @Transactional
    public SafetyChecklistResponse complete(User user, Long checklistId, List<String> confirmedItems,
                                            List<MultipartFile> photos) {
        Professional professional = access.requireProfessional(user);
        SafetyChecklist checklist = requireAccessible(professional, checklistId);
        if (checklist.getStatus() == ChecklistStatus.COMPLETED) {
            throw alreadyCompleted(checklist);
        }
        Set<ChecklistItem> items = requireAllItems(confirmedItems);
        List<MultipartFile> files = requirePhotos(photos);

        // 1) Grava os arquivos. Se a transação não confirmar (falha ou conclusão perdida para
        //    outro profissional), eles são apagados — nenhuma foto órfã fica no disco.
        List<String> keys = new ArrayList<>();
        registerCleanupOnRollback(keys);
        try {
            for (MultipartFile file : files) {
                keys.add(storageService.storePrivate(file, "checklists"));
            }
        } catch (RuntimeException e) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                keys.forEach(storageService::deletePrivate);
            }
            throw e;
        }

        // 2) Conclusão atômica: só o primeiro envio válido muda PENDING → COMPLETED.
        Instant now = clock.instant();
        int changed = checklistRepository.completeIfPending(
                checklistId, user, user.getName(), professional.roleLabel(),
                now, now.plus(Duration.ofDays(retentionDays)));
        if (changed == 0) {
            SafetyChecklist current = checklistRepository.findById(checklistId).orElseThrow();
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                keys.forEach(storageService::deletePrivate);
            }
            throw alreadyCompleted(current);
        }

        // 3) Auditoria: itens confirmados + fotos (quem enviou e quando).
        SafetyChecklist completed = checklistRepository.findById(checklistId).orElseThrow();
        completed.getConfirmedItems().addAll(items);
        for (int i = 0; i < keys.size(); i++) {
            SafetyChecklistPhoto photo = new SafetyChecklistPhoto();
            photo.setChecklist(completed);
            photo.setStorageKey(keys.get(i));
            photo.setContentType(files.get(i).getContentType());
            photo.setUploadedBy(user);
            photo.setUploadedAt(now);
            completed.getPhotos().add(photo);
        }
        checklistRepository.save(completed);
        events.publishEvent(new TripEvents.ChecklistCompleted(completed.getId()));
        return toResponse(completed, user, true);
    }

    @Override
    @Transactional
    public int purgeExpiredPhotos() {
        Instant now = clock.instant();
        int purged = 0;
        Map<Long, SafetyChecklist> touched = new HashMap<>();
        for (SafetyChecklistPhoto photo : photoRepository.findExpired(now)) {
            // Arquivo já inexistente conta como removido; falha de disco fica para a próxima rodada.
            if (!storageService.deletePrivate(photo.getStorageKey())) {
                continue;
            }
            photo.setStorageKey(null);
            photo.setPurgedAt(now);
            photoRepository.save(photo);
            touched.put(photo.getChecklist().getId(), photo.getChecklist());
            purged++;
        }
        for (SafetyChecklist checklist : touched.values()) {
            boolean allGone = checklist.getPhotos().stream().noneMatch(SafetyChecklistPhoto::isAvailable);
            if (allGone && checklist.getPhotosPurgedAt() == null) {
                checklist.setPhotosPurgedAt(now);
                checklistRepository.save(checklist);
            }
        }
        return purged;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianSafetyCheckResponse> recentForGuardian(User guardianUser, Long dependentId) {
        GuardianProfile guardian = guardianRepository.findByUserId(guardianUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Apenas responsáveis."));
        List<Dependent> dependents;
        if (dependentId != null) {
            dependents = List.of(dependentRepository.findActiveByIdAndGuardian(dependentId, guardian.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Dependente", dependentId)));
        } else {
            dependents = dependentRepository.findActiveByGuardian(guardian.getId());
        }

        Instant since = clock.instant().minus(GUARDIAN_WINDOW);
        Map<Long, Instant> enrolledSinceByTrip = new HashMap<>();
        for (Dependent dependent : dependents) {
            Enrollment enrollment = enrollmentRepository
                    .findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(dependent.getId(), guardian.getId())
                    .orElse(null);
            if (enrollment == null) {
                continue; // só com contrato ativo
            }
            for (Trip trip : tripRepository.findActiveByTransporter(enrollment.getTransporter().getId())) {
                if (trip.includesDependent(dependent.getId())) {
                    // Não mostra conferências de antes de o filho entrar no transporte.
                    enrolledSinceByTrip.merge(trip.getId(), enrollment.getCreatedAt(),
                            (a, b) -> a.isBefore(b) ? a : b);
                }
            }
        }
        if (enrolledSinceByTrip.isEmpty()) {
            return List.of();
        }
        return checklistRepository.findCompletedForTripsSince(enrolledSinceByTrip.keySet(), since).stream()
                .filter(c -> !c.getCompletedAt().isBefore(enrolledSinceByTrip.get(c.getRun().getTrip().getId())))
                .limit(GUARDIAN_LIMIT)
                .map(c -> new GuardianSafetyCheckResponse(
                        c.getId(),
                        c.getRun().getTrip().getName(),
                        c.getRun().getFinishedAt(),
                        c.getCompletedAt(),
                        c.getCompletedByName(),
                        c.getCompletedByRole()))
                .toList();
    }

    // ----- Internos -----

    private SafetyChecklist requireAccessible(Professional professional, Long checklistId) {
        SafetyChecklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> ResourceNotFoundException.of("Checklist", checklistId));
        if (!access.canAccess(professional, checklist.getRun().getTrip())) {
            // Mesmo 404 de "não existe": não revela checklists de outros percursos/transportadores.
            throw ResourceNotFoundException.of("Checklist", checklistId);
        }
        return checklist;
    }

    private Set<ChecklistItem> requireAllItems(List<String> confirmed) {
        Set<ChecklistItem> items = EnumSet.noneOf(ChecklistItem.class);
        if (confirmed != null) {
            for (String code : confirmed) {
                try {
                    items.add(ChecklistItem.valueOf(code.trim()));
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw new BusinessException("Item de checklist desconhecido: " + code);
                }
            }
        }
        if (!items.containsAll(Arrays.asList(ChecklistItem.values()))) {
            throw new BusinessException("Confirme todos os itens do checklist antes de concluir.");
        }
        return items;
    }

    private List<MultipartFile> requirePhotos(List<MultipartFile> photos) {
        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.isEmpty()) {
            throw new BusinessException("Tire ao menos uma foto do interior da van para concluir a verificação.");
        }
        if (files.size() > maxPhotos) {
            throw new BusinessException("Envie no máximo " + maxPhotos + " fotos.");
        }
        return files;
    }

    private void registerCleanupOnRollback(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    keys.forEach(storageService::deletePrivate);
                }
            }
        });
    }

    private ChecklistAlreadyCompletedException alreadyCompleted(SafetyChecklist checklist) {
        String who = checklist.getCompletedByName() != null
                ? checklist.getCompletedByName() + " — " + checklist.getCompletedByRole()
                : "outro profissional";
        String when = checklist.getCompletedAt() != null ? " em " + DATE_TIME.format(checklist.getCompletedAt()) : "";
        return new ChecklistAlreadyCompletedException(
                "Esta verificação já foi realizada por " + who + when + ".");
    }

    private SafetyChecklistResponse toResponse(SafetyChecklist checklist, User viewer, boolean withPhotos) {
        Set<ChecklistItem> confirmed = new HashSet<>(checklist.getConfirmedItems());
        List<ChecklistItemResponse> items = Arrays.stream(ChecklistItem.values())
                .map(item -> new ChecklistItemResponse(item.name(), item.label(), confirmed.contains(item)))
                .toList();
        List<ChecklistPhotoResponse> photos = withPhotos
                ? checklist.getPhotos().stream()
                        .map(p -> new ChecklistPhotoResponse(
                                p.getId(),
                                p.isAvailable() ? urlSigner.signedPath(p.getId()) : null,
                                p.isAvailable(),
                                p.getUploadedAt()))
                        .toList()
                : List.of();
        var run = checklist.getRun();
        return new SafetyChecklistResponse(
                checklist.getId(),
                run.getId(),
                run.getTrip().getId(),
                run.getTrip().getName(),
                run.getServiceDate(),
                run.getFinishedAt(),
                checklist.getStatus(),
                checklist.getCompletedByName(),
                checklist.getCompletedByRole(),
                checklist.getCompletedAt(),
                checklist.getCompletedBy() != null && checklist.getCompletedBy().getId().equals(viewer.getId()),
                items,
                photos,
                checklist.getPhotos().size(),
                checklist.getPhotosPurgeAt(),
                checklist.getPhotosPurgedAt(),
                checklist.getGuardiansNotified());
    }
}
