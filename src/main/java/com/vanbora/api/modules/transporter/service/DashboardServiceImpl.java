package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.DashboardResponse;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compõe o painel do transportador a partir de matrículas, solicitações e avisos. */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TransporterProfileRepository transporterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final HireRequestRepository hireRequestRepository;
    private final NoticeRepository noticeRepository;
    private final AbsenceRepository absenceRepository;
    private final ContractRepository contractRepository;

    public DashboardServiceImpl(
            TransporterProfileRepository transporterRepository,
            EnrollmentRepository enrollmentRepository,
            HireRequestRepository hireRequestRepository,
            NoticeRepository noticeRepository,
            AbsenceRepository absenceRepository,
            ContractRepository contractRepository) {
        this.transporterRepository = transporterRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.hireRequestRepository = hireRequestRepository;
        this.noticeRepository = noticeRepository;
        this.absenceRepository = absenceRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    public DashboardResponse getDashboard(Long transporterUserId) {
        TransporterProfile transporter = transporterRepository.findByUserId(transporterUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", transporterUserId));
        Long transporterId = transporter.getId();

        // Apenas matrículas ATIVAS, deduplicadas por aluno (cancelados/recontratações não contam 2x).
        Collection<Enrollment> enrollments =
                distinctByDependent(enrollmentRepository.findByTransporterIdAndActiveTrue(transporterId));
        int total = enrollments.size();
        int confirmed = countConfirmedToday(enrollments);
        int absent = total - confirmed;

        List<HireRequestResponse> hireRequests = hireRequestRepository
                .findByTransporterIdAndStatus(transporterId, HireStatus.PENDING).stream()
                .filter(h -> !h.isOverdue())
                .map(HireRequestResponse::from)
                .toList();

        long cancelledContracts =
                contractRepository.countByTransporterIdAndStatus(transporterId, ContractStatus.CANCELLED);

        NoticeResponse recentNotice = noticeRepository
                .findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .findFirst()
                .map(n -> NoticeResponse.from(n, java.util.List.of(), java.time.Instant.now(), 0, 0L))
                .orElse(null);

        return new DashboardResponse(
                total, confirmed, absent, (int) cancelledContracts, hireRequests, recentNotice);
    }

    /** Mantém uma matrícula por aluno (defesa contra dados legados com duplicatas ativas). */
    private Collection<Enrollment> distinctByDependent(List<Enrollment> enrollments) {
        Map<Long, Enrollment> byDependent = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            byDependent.putIfAbsent(e.getDependent().getId(), e);
        }
        return byDependent.values();
    }

    private int countConfirmedToday(Collection<Enrollment> enrollments) {
        LocalDate today = LocalDate.now();
        boolean weekend =
                today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;

        int confirmed = 0;
        for (Enrollment enrollment : enrollments) {
            if (weekend || isPresentToday(enrollment, today)) {
                confirmed++;
            }
        }
        return confirmed;
    }

    /** Presente hoje = não há falta avisada para a data (default: presente). */
    private boolean isPresentToday(Enrollment enrollment, LocalDate today) {
        return !absenceRepository.existsByEnrollmentIdAndDate(enrollment.getId(), today);
    }
}
