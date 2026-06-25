package com.vanbora.api.modules.guardian.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse.DayAttendance;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse.TransporterNotice;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.guardian.dto.GuardianTrackingResponse;
import com.vanbora.api.modules.guardian.dto.UpdateAddressRequest;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import com.vanbora.api.modules.payment.repository.PaymentRepository;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final PaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final RouteStopRepository routeStopRepository;
    private final GuardianAddressService guardianAddressService;

    public GuardianServiceImpl(
            GuardianProfileRepository guardianRepository,
            DependentRepository dependentRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentRepository paymentRepository,
            NoticeRepository noticeRepository,
            RouteStopRepository routeStopRepository,
            GuardianAddressService guardianAddressService) {
        this.guardianRepository = guardianRepository;
        this.dependentRepository = dependentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.noticeRepository = noticeRepository;
        this.routeStopRepository = routeStopRepository;
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
        return DependentResponse.from(dependentRepository.save(dependent));
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianDashboardResponse getDashboard(Long userId) {
        GuardianProfile guardian = resolve(userId);

        Optional<Enrollment> enrollmentOpt =
                enrollmentRepository.findFirstByDependentGuardianIdAndActiveTrue(guardian.getId());

        if (enrollmentOpt.isEmpty()) {
            return new GuardianDashboardResponse(false, null, List.of(), null, null);
        }

        Enrollment enrollment = enrollmentOpt.get();
        List<DayAttendance> week = buildWeek(enrollment);
        PaymentResponse nextPayment = findNextPayment(guardian.getId()).map(PaymentResponse::from).orElse(null);
        TransporterNotice notice = latestNotice(enrollment.getTransporter().getId());

        return new GuardianDashboardResponse(
                true, enrollment.getDependent().getName(), week, nextPayment, notice);
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
    public GuardianTrackingResponse getTracking(Long userId) {
        GuardianProfile guardian = resolve(userId);

        Optional<Enrollment> enrollmentOpt =
                enrollmentRepository.findFirstByDependentGuardianIdAndActiveTrue(guardian.getId());
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

    private List<DayAttendance> buildWeek(Enrollment enrollment) {
        Map<DayOfWeek, Boolean> presence = new LinkedHashMap<>();
        enrollment.getAttendance().forEach(a -> presence.put(a.getDayOfWeek(), a.isPresent()));

        return WEEKDAY_LABELS.entrySet().stream()
                .map(entry -> new DayAttendance(entry.getValue(), presence.get(entry.getKey())))
                .toList();
    }

    private Optional<Payment> findNextPayment(Long guardianId) {
        return paymentRepository
                .findByEnrollmentDependentGuardianIdOrderByReferenceMonthDesc(guardianId).stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .findFirst();
    }

    private TransporterNotice latestNotice(Long transporterId) {
        return noticeRepository.findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .findFirst()
                .map(n -> new TransporterNotice("Avisos do Transportador", n.getMessage()))
                .orElse(null);
    }

    private List<DependentResponse> mapDependents(Long guardianId) {
        return dependentRepository.findByGuardianId(guardianId).stream()
                .map(DependentResponse::from)
                .toList();
    }

    private GuardianProfile resolve(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }
}
