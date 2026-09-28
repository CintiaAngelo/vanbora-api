package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.chat.service.ChatMembershipService;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.monitor.service.MonitorAccountService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.trip.domain.SafetyChecklist;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.domain.TripRun;
import com.vanbora.api.modules.trip.dto.TripRequest;
import com.vanbora.api.modules.trip.dto.TripResponse;
import com.vanbora.api.modules.trip.dto.TripResponse.TripMonitorResponse;
import com.vanbora.api.modules.trip.dto.TripRunResponse;
import com.vanbora.api.modules.trip.repository.SafetyChecklistRepository;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.trip.repository.TripRunRepository;
import com.vanbora.api.modules.trip.service.TripAccessService.Professional;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.ChecklistStatus;
import com.vanbora.api.shared.enums.TripRunStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos percursos: cadastro pelo transportador e ciclo de execução pela equipe. */
@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripRunRepository runRepository;
    private final SafetyChecklistRepository checklistRepository;
    private final HelperRepository helperRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TripAccessService access;
    private final ChatMembershipService chatMembershipService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public TripServiceImpl(TripRepository tripRepository,
                           TripRunRepository runRepository,
                           SafetyChecklistRepository checklistRepository,
                           HelperRepository helperRepository,
                           EnrollmentRepository enrollmentRepository,
                           TripAccessService access,
                           ChatMembershipService chatMembershipService,
                           ApplicationEventPublisher events) {
        this(tripRepository, runRepository, checklistRepository, helperRepository, enrollmentRepository,
                access, chatMembershipService, events, Clock.systemDefaultZone());
    }

    TripServiceImpl(TripRepository tripRepository,
                    TripRunRepository runRepository,
                    SafetyChecklistRepository checklistRepository,
                    HelperRepository helperRepository,
                    EnrollmentRepository enrollmentRepository,
                    TripAccessService access,
                    ChatMembershipService chatMembershipService,
                    ApplicationEventPublisher events,
                    Clock clock) {
        this.tripRepository = tripRepository;
        this.runRepository = runRepository;
        this.checklistRepository = checklistRepository;
        this.helperRepository = helperRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.access = access;
        this.chatMembershipService = chatMembershipService;
        this.events = events;
        this.clock = clock;
    }

    // ----- Cadastro (transportador) -----

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> list(User user) {
        Professional professional = access.requireProfessional(user);
        return access.tripsOf(professional).stream()
                .map(trip -> toResponse(trip, professional))
                .toList();
    }

    @Override
    @Transactional
    public TripResponse create(User transporterUser, TripRequest request) {
        Professional professional = requireTransporter(transporterUser);
        Trip trip = new Trip();
        trip.setTransporter(professional.transporter());
        trip.setActive(true);
        apply(trip, request, professional);
        trip = tripRepository.save(trip);
        chatMembershipService.syncTransporter(professional.transporter().getId());
        return toResponse(trip, professional);
    }

    @Override
    @Transactional
    public TripResponse update(User transporterUser, Long tripId, TripRequest request) {
        Professional professional = requireTransporter(transporterUser);
        Trip trip = ownedActiveTrip(professional, tripId);
        apply(trip, request, professional);
        tripRepository.save(trip);
        // Monitor tirado do percurso perde na hora o acesso aos alunos/checklists/conversas dele.
        chatMembershipService.syncTransporter(professional.transporter().getId());
        return toResponse(trip, professional);
    }

    @Override
    @Transactional
    public void delete(User transporterUser, Long tripId) {
        Professional professional = requireTransporter(transporterUser);
        Trip trip = ownedActiveTrip(professional, tripId);
        if (runRepository.findFirstByTripIdAndStatus(trip.getId(), TripRunStatus.IN_PROGRESS).isPresent()) {
            throw new BusinessException("Encerre ou cancele o percurso em andamento antes de excluí-lo.");
        }
        trip.setActive(false);
        tripRepository.save(trip);
        chatMembershipService.syncTransporter(professional.transporter().getId());
    }

    private void apply(Trip trip, TripRequest request, Professional professional) {
        trip.setName(request.name().trim());

        Set<Helper> monitors = new LinkedHashSet<>();
        for (Long helperId : distinct(request.monitorIds())) {
            Helper helper = helperRepository.findById(helperId)
                    .filter(h -> h.getTransporter().getId().equals(professional.transporter().getId()))
                    .orElseThrow(() -> ResourceNotFoundException.of("Ajudante", helperId));
            if (!MonitorAccountService.isMonitorRole(helper.getRole()) || helper.getUser() == null) {
                throw new BusinessException(
                        helper.getName() + " não tem acesso de monitor ao app. Libere o acesso no cadastro do ajudante.");
            }
            monitors.add(helper);
        }
        trip.getMonitors().clear();
        trip.getMonitors().addAll(monitors);

        boolean all = request.allStudents() == null || request.allStudents();
        trip.setAllStudents(all);
        trip.getStudents().clear();
        if (!all) {
            Map<Long, Dependent> ownStudents = enrollmentRepository
                    .findByTransporterIdAndActiveTrue(professional.transporter().getId()).stream()
                    .map(Enrollment::getDependent)
                    .collect(Collectors.toMap(Dependent::getId, Function.identity(), (a, b) -> a));
            for (Long dependentId : distinct(request.studentIds())) {
                Dependent dependent = ownStudents.get(dependentId);
                if (dependent == null) {
                    // Só alunos com contrato ativo com ESTE transportador (sem vazar alheios).
                    throw ResourceNotFoundException.of("Aluno", dependentId);
                }
                trip.getStudents().add(dependent);
            }
            if (trip.getStudents().isEmpty()) {
                throw new BusinessException("Selecione ao menos um aluno ou marque \"Todos os alunos\".");
            }
        }
    }

    // ----- Execução (transportador ou monitor escalado) -----

    @Override
    @Transactional
    public TripRunResponse start(User user, Long tripId) {
        Professional professional = access.requireProfessional(user);
        access.requireTrip(professional, tripId);
        // Trava o percurso: dois "iniciar" simultâneos não criam duas execuções.
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> ResourceNotFoundException.of("Percurso", tripId));
        if (!trip.isActive()) {
            throw ResourceNotFoundException.of("Percurso", tripId);
        }
        TripRun running = runRepository.findFirstByTripIdAndStatus(tripId, TripRunStatus.IN_PROGRESS).orElse(null);
        if (running != null) {
            return TripRunResponse.from(running, null); // já iniciado por outro profissional
        }

        TripRun run = new TripRun();
        run.setTrip(trip);
        run.setTransporter(trip.getTransporter());
        run.setServiceDate(LocalDate.now(clock));
        run.setStatus(TripRunStatus.IN_PROGRESS);
        run.setStartedAt(clock.instant());
        run.setStartedBy(user);
        for (Helper helper : trip.getMonitors()) {
            if (helper.hasActiveAccess()) {
                run.getMonitorsOnDuty().add(helper.getUser());
            }
        }
        return TripRunResponse.from(runRepository.save(run), null);
    }

    @Override
    @Transactional
    public TripRunResponse finish(User user, Long runId) {
        Professional professional = access.requireProfessional(user);
        requireAccessibleRun(professional, runId);

        int changed = runRepository.finishIfInProgress(runId, user, clock.instant());
        TripRun run = runRepository.findById(runId).orElseThrow();
        if (changed == 0) {
            if (run.getStatus() == TripRunStatus.CANCELLED) {
                throw new BusinessException("Este percurso foi cancelado e não pode ser encerrado.");
            }
            // Já encerrado (ex.: motorista e monitor tocaram juntos): devolve o estado atual.
            return TripRunResponse.from(run, checklistRepository.findByRunId(runId).orElse(null));
        }

        SafetyChecklist checklist = new SafetyChecklist();
        checklist.setRun(run);
        checklist.setTransporter(run.getTransporter());
        checklist.setStatus(ChecklistStatus.PENDING);
        checklist = checklistRepository.save(checklist);
        events.publishEvent(new TripEvents.ChecklistOpened(checklist.getId()));
        return TripRunResponse.from(run, checklist);
    }

    @Override
    @Transactional
    public TripRunResponse cancel(User user, Long runId) {
        Professional professional = access.requireProfessional(user);
        requireAccessibleRun(professional, runId);
        if (runRepository.cancelIfInProgress(runId, clock.instant()) == 0) {
            throw new BusinessException("Só é possível cancelar um percurso em andamento.");
        }
        return TripRunResponse.from(runRepository.findById(runId).orElseThrow(), null);
    }

    // ----- Internos -----

    private Professional requireTransporter(User user) {
        Professional professional = access.requireProfessional(user);
        if (!professional.isTransporter()) {
            throw new AccessDeniedException("Apenas o transportador gerencia percursos.");
        }
        return professional;
    }

    private Trip ownedActiveTrip(Professional professional, Long tripId) {
        return tripRepository.findByIdAndTransporterId(tripId, professional.transporter().getId())
                .filter(Trip::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Percurso", tripId));
    }

    private void requireAccessibleRun(Professional professional, Long runId) {
        TripRun run = runRepository.findById(runId)
                .orElseThrow(() -> ResourceNotFoundException.of("Percurso em andamento", runId));
        if (!access.canAccess(professional, run.getTrip())) {
            throw ResourceNotFoundException.of("Percurso em andamento", runId);
        }
    }

    private TripResponse toResponse(Trip trip, Professional professional) {
        List<TripMonitorResponse> monitors = trip.getMonitors().stream()
                .map(h -> new TripMonitorResponse(h.getId(), h.getName(), h.getPhotoUrl(), h.hasActiveAccess()))
                .toList();
        List<Long> studentIds = professional.isTransporter() && !trip.isAllStudents()
                ? trip.getStudents().stream().map(Dependent::getId).toList()
                : null;
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.isAllStudents(),
                access.activeEnrollmentsOf(trip).size(),
                monitors,
                studentIds,
                currentRun(trip));
    }

    /** Execução em andamento, ou a última de hoje (para mostrar "encerrado"); senão nenhuma. */
    private TripRunResponse currentRun(Trip trip) {
        return runRepository.findFirstByTripIdOrderByStartedAtDesc(trip.getId())
                .filter(run -> run.getStatus() == TripRunStatus.IN_PROGRESS
                        || run.getServiceDate().equals(LocalDate.now(clock)))
                .map(run -> TripRunResponse.from(run, checklistRepository.findByRunId(run.getId()).orElse(null)))
                .orElse(null);
    }

    private static List<Long> distinct(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
}
