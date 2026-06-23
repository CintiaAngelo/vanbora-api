package com.vanbora.api.modules.notice.service;

import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos casos de uso de avisos. */
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final TransporterProfileRepository transporterRepository;
    private final EnrollmentRepository enrollmentRepository;

    public NoticeServiceImpl(NoticeRepository noticeRepository,
                             TransporterProfileRepository transporterRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.noticeRepository = noticeRepository;
        this.transporterRepository = transporterRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoticeResponse> listMyNotices(Long transporterUserId) {
        TransporterProfile transporter = resolve(transporterUserId);
        return noticeRepository.findByTransporterIdOrderByCreatedAtDesc(transporter.getId()).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NoticeResponse create(Long transporterUserId, CreateNoticeRequest request) {
        TransporterProfile transporter = resolve(transporterUserId);

        Notice notice = new Notice();
        notice.setTransporter(transporter);
        notice.setMessage(request.message());
        notice.setRecipientsCount((int) enrollmentRepository.countByTransporterId(transporter.getId()));

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    private TransporterProfile resolve(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }
}
