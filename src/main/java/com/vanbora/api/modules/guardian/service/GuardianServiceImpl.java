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
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import com.vanbora.api.modules.payment.repository.PaymentRepository;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
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

    public GuardianServiceImpl(
            GuardianProfileRepository guardianRepository,
            DependentRepository dependentRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentRepository paymentRepository,
            NoticeRepository noticeRepository) {
        this.guardianRepository = guardianRepository;
        this.dependentRepository = dependentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.noticeRepository = noticeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianProfileResponse getMyProfile(Long userId) {
        GuardianProfile guardian = resolve(userId);
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
