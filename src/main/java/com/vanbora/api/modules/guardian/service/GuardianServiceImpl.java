package com.vanbora.api.modules.guardian.service;

import com.vanbora.api.modules.enrollment.domain.Absence;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.dto.AttendanceDayResponse;
import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse.DayAttendance;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse.NextPayment;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse.TransporterNotice;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.guardian.dto.GuardianTrackingResponse;
import com.vanbora.api.modules.guardian.dto.PaymentScheduleItemResponse;
import com.vanbora.api.modules.guardian.dto.UpdateAddressRequest;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import com.vanbora.api.modules.payment.repository.PaymentRepository;
import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.ReviewRepository;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos casos de uso do responsável. */
@Service
public class GuardianServiceImpl implements GuardianService {

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = new LinkedHashMap<>();

    static {
        WEEKDAY_LABELS.put(DayOfWeek.MONDAY, "SEG");
        WEEKDAY_LABELS.put(DayOfWeek.TUESDAY, "TER");
        WEEKDAY_LABELS.put(DayOfWeek.WEDNESDAY, "QUA");
        WEEKDAY_LABELS.put(DayOfWeek.THURSDAY, "QUI");
        WEEKDAY_LABELS.put(DayOfWeek.FRIDAY, "SEX");
    }

    private final GuardianProfileRepository guardianRepository;
    private final DependentRepository dependentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AbsenceRepository absenceRepository;
    private final PaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final SchoolRepository schoolRepository;
    private final GuardianAddressService guardianAddressService;

    public GuardianServiceImpl(
            GuardianProfileRepository guardianRepository,
            DependentRepository dependentRepository,
            EnrollmentRepository enrollmentRepository,
            AbsenceRepository absenceRepository,
            PaymentRepository paymentRepository,
            NoticeRepository noticeRepository,
            RouteStopRepository routeStopRepository,
            ReviewRepository reviewRepository,
            ContractRepository contractRepository,
            SchoolRepository schoolRepository,
            GuardianAddressService guardianAddressService) {
        this.guardianRepository = guardianRepository;
        this.dependentRepository = dependentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.absenceRepository = absenceRepository;
        this.paymentRepository = paymentRepository;
        this.noticeRepository = noticeRepository;
        this.routeStopRepository = routeStopRepository;
        this.reviewRepository = reviewRepository;
        this.contractRepository = contractRepository;
        this.schoolRepository = schoolRepository;
        this.guardianAddressService = guardianAddressService;
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianProfileResponse getMyProfile(Long userId) {
        GuardianProfile guardian = resolve(userId);
        return GuardianProfileResponse.from(guardian, mapDependents(guardian.getId()));
    }

    @Override
    @Transactional
    public GuardianProfileResponse updateAddress(Long userId, UpdateAddressRequest request) {
        GuardianProfile guardian = resolve(userId);
        guardianAddressService.apply(
                guardian, request.pickup(), request.deliverySameAsPickup(), request.delivery());
        guardianRepository.save(guardian);
        return GuardianProfileResponse.from(guardian, mapDependents(guardian.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependentResponse> listDependents(Long userId) {
        GuardianProfile guardian = resolve(userId);
        return mapDependents(guardian.getId());
    }

    @Override
    @Transactional
    public DependentResponse addDependent(Long userId, CreateDependentRequest request) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = new Dependent();
        dependent.setGuardian(guardian);
        dependent.setName(request.name());
        dependent.setSchool(request.school());
        dependent.setSchoolRef(resolveSchool(request.schoolId()));
        return DependentResponse.from(dependentRepository.save(dependent));
    }

    /** Resolve a escola do catálogo pelo id (null se não informado/não encontrado). */
    private School resolveSchool(Long schoolId) {
        return schoolId == null ? null : schoolRepository.findById(schoolId).orElse(null);
    }

    @Override
    @Transactional
    public DependentResponse updateDependent(Long userId, Long dependentId, CreateDependentRequest request) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = dependentRepository.findActiveByIdAndGuardian(dependentId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Dependente", dependentId));
        dependent.setName(request.name().trim());
        dependent.setSchool(request.school().trim());
        if (request.schoolId() != null) {
            dependent.setSchoolRef(resolveSchool(request.schoolId()));
        }
        return DependentResponse.from(dependentRepository.save(dependent));
    }

    @Override
    @Transactional
    public void deleteDependent(Long userId, Long dependentId) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = dependentRepository.findActiveByIdAndGuardian(dependentId, guardian.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Dependente", dependentId));
        boolean hasActiveTransport = enrollmentRepository
                .findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(dependentId, guardian.getId())
                .isPresent();
        if (hasActiveTransport) {
            throw new BusinessException("Cancele o transporte deste dependente antes de excluí-lo.");
        }
        dependent.setArchived(true);
        dependentRepository.save(dependent);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianDashboardResponse getDashboard(Long userId, Long dependentId) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);

        if (dependent == null) {
            // Responsável sem dependentes cadastrados.
            return new GuardianDashboardResponse(
                    false, null, null, null, null, null, false, null, List.of(), null, null);
        }

        Long depId = dependent.getId();
        LocalDate today = LocalDate.now();
        Long pendingContractId = contractRepository
                .findFirstByGuardianIdAndDependentIdAndStatusOrderByIdDesc(
                        guardian.getId(), depId, ContractStatus.PENDING_SIGNATURE)
                .map(Contract::getId)
                .orElse(null);

        Optional<Enrollment> enrollmentOpt = activeEnrollment(guardian, dependent);
        if (enrollmentOpt.isEmpty()) {
            // Dependente sem transportador (ou em fase de contratação).
            return new GuardianDashboardResponse(
                    false, depId, dependent.getName(), pendingContractId,
                    null, null, false, null, List.of(), null, null);
        }

        Enrollment enrollment = enrollmentOpt.get();
        TransporterProfile transporter = enrollment.getTransporter();

        List<DayAttendance> week = buildWeek(enrollment, today);
        Boolean goingToday = isWeekend(today) ? null : !hasAbsence(enrollment, today);
        NextPayment nextPayment = computeNextPayment(enrollment, depId, today);
        TransporterNotice notice = latestNotice(transporter.getId());
        boolean reviewDue = isReviewDue(transporter.getId(), guardian.getId());

        return new GuardianDashboardResponse(
                true,
                depId,
                dependent.getName(),
                pendingContractId,
                transporter.getId(),
                transporter.getUser().getName(),
                reviewDue,
                goingToday,
                week,
                nextPayment,
                notice);
    }

    /** Resolve o dependente selecionado (valida posse). dependentId null → primeiro do responsável. */
    private Dependent resolveDependent(GuardianProfile guardian, Long dependentId) {
        if (dependentId != null) {
            return dependentRepository.findActiveByIdAndGuardian(dependentId, guardian.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Dependente", dependentId));
        }
        return dependentRepository.findActiveByGuardian(guardian.getId()).stream().findFirst().orElse(null);
    }

    /** Matrícula ativa do dependente (vazio se ele ainda não tem transportador). */
    private Optional<Enrollment> activeEnrollment(GuardianProfile guardian, Dependent dependent) {
        if (dependent == null) {
            return Optional.empty();
        }
        return enrollmentRepository.findFirstByDependentIdAndDependentGuardianIdAndActiveTrue(
                dependent.getId(), guardian.getId());
    }

    /** Pesquisa vencida: nunca avaliou OU a última avaliação passou de 3 meses. */
    private boolean isReviewDue(Long transporterId, Long guardianId) {
        return reviewRepository
                .findFirstByTransporterIdAndGuardianIdOrderByCreatedAtDesc(transporterId, guardianId)
                .map(r -> r.getCreatedAt().isBefore(ZonedDateTime.now().minusMonths(3).toInstant()))
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(Long userId) {
        GuardianProfile guardian = resolve(userId);
        return paymentRepository
                .findByEnrollmentDependentGuardianIdOrderByReferenceMonthDesc(guardian.getId()).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentScheduleItemResponse> listPaymentSchedule(Long userId, Long dependentId) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);
        Optional<Enrollment> enrollmentOpt = activeEnrollment(guardian, dependent);
        if (enrollmentOpt.isEmpty()) {
            return List.of();
        }
        Enrollment enrollment = enrollmentOpt.get();
        LocalDate today = LocalDate.now();

        List<Payment> real = paymentRepository
                .findByEnrollmentDependentIdOrderByReferenceMonthDesc(dependent.getId());

        List<PaymentScheduleItemResponse> items = new ArrayList<>();
        Set<String> realMonths = new HashSet<>();
        for (Payment p : real) {
            realMonths.add(p.getReferenceMonth());
            items.add(new PaymentScheduleItemResponse(
                    p.getId(), p.getReferenceMonth(), p.getAmount(),
                    effectiveStatus(p, today), p.getDueDate(), false));
        }

        // Projeta os próximos 6 meses ainda não cobrados.
        YearMonth latest = real.isEmpty() ? YearMonth.from(today).minusMonths(1) : maxMonth(real);
        int dueDay = dueDayOf(real);
        for (int i = 1; i <= 6; i++) {
            YearMonth ym = latest.plusMonths(i);
            if (realMonths.contains(ym.toString())) {
                continue;
            }
            items.add(new PaymentScheduleItemResponse(
                    null, ym.toString(), enrollment.getMonthlyFee(), "SCHEDULED",
                    ym.atDay(Math.min(dueDay, ym.lengthOfMonth())), true));
        }

        items.sort(Comparator.comparing(PaymentScheduleItemResponse::referenceMonth));
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDayResponse> listAttendance(Long userId, Long dependentId) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);
        return activeEnrollment(guardian, dependent)
                .map(e -> upcomingSchoolDays(e, LocalDate.now()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDayResponse> getWeek(Long userId, Long dependentId, LocalDate date) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);
        Enrollment enrollment = activeEnrollment(guardian, dependent).orElse(null);
        if (enrollment == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<AttendanceDayResponse> days = new ArrayList<>();
        int i = 0;
        for (Map.Entry<DayOfWeek, String> entry : WEEKDAY_LABELS.entrySet()) {
            LocalDate d = monday.plusDays(i++);
            days.add(new AttendanceDayResponse(
                    d.toString(),
                    entry.getValue(),
                    !hasAbsence(enrollment, d),
                    d.equals(today),
                    !d.isBefore(today)));
        }
        return days;
    }

    @Override
    @Transactional
    public List<AttendanceDayResponse> setGoing(Long userId, Long dependentId, LocalDate date, boolean going) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);
        Enrollment enrollment = activeEnrollment(guardian, dependent)
                .orElseThrow(() -> new BusinessException("Este dependente ainda não tem um transporte contratado."));
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new BusinessException("Não é possível alterar dias que já passaram.");
        }
        if (isWeekend(date)) {
            throw new BusinessException("Não há transporte no fim de semana.");
        }
        Optional<Absence> existing =
                absenceRepository.findByEnrollmentIdAndDate(enrollment.getId(), date);
        if (going) {
            existing.ifPresent(absenceRepository::delete);
        } else if (existing.isEmpty()) {
            Absence absence = new Absence();
            absence.setEnrollment(enrollment);
            absence.setDate(date);
            absenceRepository.save(absence);
        }
        return upcomingSchoolDays(enrollment, today);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianTrackingResponse getTracking(Long userId, Long dependentId) {
        GuardianProfile guardian = resolve(userId);
        Dependent dependent = resolveDependent(guardian, dependentId);

        Optional<Enrollment> enrollmentOpt = activeEnrollment(guardian, dependent);
        if (enrollmentOpt.isEmpty()) {
            return GuardianTrackingResponse.none();
        }

        Enrollment enrollment = enrollmentOpt.get();
        TransporterProfile transporter = enrollment.getTransporter();
        Long myDependentId = enrollment.getDependent().getId();
        String mySchool = enrollment.getDependent().getSchool();

        List<RouteStop> stops =
                routeStopRepository.findByTransporterIdOrderByPositionAsc(transporter.getId());

        // Apenas a parada do próprio dependente e a escola — nada de outros responsáveis.
        RouteStop myStop = stops.stream()
                .filter(s -> s.getDependent() != null && myDependentId.equals(s.getDependent().getId()))
                .findFirst()
                .orElse(null);
        // A escola do próprio aluno (casando pelo nome); cai na primeira escola se não achar.
        RouteStop schoolStop = stops.stream()
                .filter(s -> s.getStatus() == RouteStopStatus.SCHOOL)
                .filter(s -> mySchool != null && mySchool.equalsIgnoreCase(s.getLabel()))
                .findFirst()
                .orElseGet(() -> stops.stream()
                        .filter(s -> s.getStatus() == RouteStopStatus.SCHOOL)
                        .findFirst()
                        .orElse(null));

        // Ordem de embarque entre os alunos que VÃO hoje (por posição da rota).
        List<RouteStop> activePickups = stops.stream()
                .filter(s -> s.getStatus() == RouteStopStatus.GOING)
                .sorted(Comparator.comparingInt(RouteStop::getPosition))
                .toList();
        boolean goingToday = myStop != null && myStop.getStatus() == RouteStopStatus.GOING;
        Integer myOrder = null;
        if (goingToday) {
            for (int i = 0; i < activePickups.size(); i++) {
                if (activePickups.get(i).getId().equals(myStop.getId())) {
                    myOrder = i + 1;
                    break;
                }
            }
        }
        Integer totalStops = activePickups.isEmpty() ? null : activePickups.size();

        return new GuardianTrackingResponse(
                true,
                transporter.getUser().getName(),
                enrollment.getDependent().getName(),
                transporter.getCurrentLatitude(),
                transporter.getCurrentLongitude(),
                transporter.getLocationUpdatedAt(),
                myStop != null ? RouteStopResponse.from(myStop) : null,
                schoolStop != null ? RouteStopResponse.from(schoolStop) : null,
                myOrder,
                totalStops,
                goingToday);
    }

    private static final int UPCOMING_SCHOOL_DAYS = 10;

    /** Próximos N dias letivos (seg–sex) com o status "vai / não vai". */
    private List<AttendanceDayResponse> upcomingSchoolDays(Enrollment enrollment, LocalDate today) {
        List<AttendanceDayResponse> days = new ArrayList<>();
        LocalDate cursor = today;
        while (days.size() < UPCOMING_SCHOOL_DAYS) {
            if (!isWeekend(cursor)) {
                days.add(new AttendanceDayResponse(
                        cursor.toString(),
                        WEEKDAY_LABELS.get(cursor.getDayOfWeek()),
                        !hasAbsence(enrollment, cursor),
                        cursor.equals(today),
                        true));
            }
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    /** Semana atual (seg–sex) com a presença real (default presente). */
    private List<DayAttendance> buildWeek(Enrollment enrollment, LocalDate today) {
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DayAttendance> week = new ArrayList<>();
        int i = 0;
        for (Map.Entry<DayOfWeek, String> entry : WEEKDAY_LABELS.entrySet()) {
            LocalDate date = monday.plusDays(i++);
            week.add(new DayAttendance(entry.getValue(), date.toString(), !hasAbsence(enrollment, date)));
        }
        return week;
    }

    /** Próxima mensalidade: a em aberto mais antiga; senão projeta o próximo mês. */
    private NextPayment computeNextPayment(Enrollment enrollment, Long dependentId, LocalDate today) {
        List<Payment> payments = paymentRepository
                .findByEnrollmentDependentIdOrderByReferenceMonthDesc(dependentId);
        Optional<Payment> soonestUnpaid = payments.stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .min(Comparator.comparing(Payment::getReferenceMonth));
        if (soonestUnpaid.isPresent()) {
            Payment p = soonestUnpaid.get();
            return new NextPayment(p.getReferenceMonth(), p.getAmount(), p.getDueDate(),
                    effectiveStatus(p, today), true);
        }
        YearMonth latest = payments.isEmpty() ? YearMonth.from(today).minusMonths(1) : maxMonth(payments);
        YearMonth next = latest.plusMonths(1);
        int dueDay = dueDayOf(payments);
        LocalDate due = next.atDay(Math.min(dueDay, next.lengthOfMonth()));
        return new NextPayment(next.toString(), enrollment.getMonthlyFee(), due, "SCHEDULED", false);
    }

    private String effectiveStatus(Payment p, LocalDate today) {
        if (p.getStatus() == PaymentStatus.PAID) {
            return "PAID";
        }
        if (p.getDueDate() != null && p.getDueDate().isBefore(today)) {
            return "OVERDUE";
        }
        return p.getStatus() == PaymentStatus.OVERDUE ? "OVERDUE" : "PENDING";
    }

    /** A lista vem ordenada por referenceMonth DESC, então o primeiro é o maior. */
    private YearMonth maxMonth(List<Payment> payments) {
        return YearMonth.parse(payments.get(0).getReferenceMonth());
    }

    private int dueDayOf(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getDueDate)
                .filter(d -> d != null)
                .map(LocalDate::getDayOfMonth)
                .findFirst()
                .orElse(10);
    }

    private boolean hasAbsence(Enrollment enrollment, LocalDate date) {
        return absenceRepository.existsByEnrollmentIdAndDate(enrollment.getId(), date);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private TransporterNotice latestNotice(Long transporterId) {
        return noticeRepository.findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .findFirst()
                .map(n -> new TransporterNotice("Avisos do Transportador", n.getMessage()))
                .orElse(null);
    }

    private List<DependentResponse> mapDependents(Long guardianId) {
        return dependentRepository.findActiveByGuardian(guardianId).stream()
                .map(DependentResponse::from)
                .toList();
    }

    private GuardianProfile resolve(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }
}
