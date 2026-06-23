package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.enrollment.domain.Attendance;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.hire.dto.HireRequestResponse;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.DashboardResponse;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
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

    public DashboardServiceImpl(
            TransporterProfileRepository transporterRepository,
            EnrollmentRepository enrollmentRepository,
            HireRequestRepository hireRequestRepository,
            NoticeRepository noticeRepository) {
        this.transporterRepository = transporterRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.hireRequestRepository = hireRequestRepository;
        this.noticeRepository = noticeRepository;
    }

    @Override
    public DashboardResponse getDashboard(Long transporterUserId) {
        TransporterProfile transporter = transporterRepository.findByUserId(transporterUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", transporterUserId));
        Long transporterId = transporter.getId();

        List<Enrollment> enrollments = enrollmentRepository.findByTransporterId(transporterId);
        int total = enrollments.size();
        int confirmed = countConfirmedToday(enrollments);
        int absent = total - confirmed;

        List<HireRequestResponse> hireRequests = hireRequestRepository
                .findByTransporterIdAndStatus(transporterId, HireStatus.PENDING).stream()
                .map(HireRequestResponse::from)
                .toList();

        NoticeResponse recentNotice = noticeRepository
                .findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .findFirst()
                .map(NoticeResponse::from)
                .orElse(null);

        return new DashboardResponse(total, confirmed, absent, hireRequests, recentNotice);
    }

    private int countConfirmedToday(List<Enrollment> enrollments) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        boolean weekend = today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY;

        int confirmed = 0;
        for (Enrollment enrollment : enrollments) {
            if (weekend || isPresentOn(enrollment, today)) {
                confirmed++;
            }
        }
        return confirmed;
    }

    private boolean isPresentOn(Enrollment enrollment, DayOfWeek day) {
        return enrollment.getAttendance().stream()
                .filter(a -> a.getDayOfWeek() == day)
                .findFirst()
                .map(Attendance::isPresent)
                .orElse(true);
    }
}
