package com.vanbora.api.modules.notice.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.domain.NoticeComment;
import com.vanbora.api.modules.notice.domain.NoticeReaction;
import com.vanbora.api.modules.notice.domain.NoticeRecipient;
import com.vanbora.api.modules.notice.domain.NoticeView;
import com.vanbora.api.modules.notice.dto.AudienceRecipientResponse;
import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.GuardianNoticeResponse;
import com.vanbora.api.modules.notice.dto.NoticeCommentResponse;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.dto.UpdateNoticeRequest;
import com.vanbora.api.modules.notice.repository.NoticeCommentRepository;
import com.vanbora.api.modules.notice.repository.NoticeReactionRepository;
import com.vanbora.api.modules.notice.repository.NoticeRecipientRepository;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.notice.repository.NoticeViewRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.NoticePriority;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação do mural de avisos: agendamento, reações, comentários e leituras. */
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReactionRepository reactionRepository;
    private final NoticeCommentRepository commentRepository;
    private final NoticeViewRepository viewRepository;
    private final NoticeRecipientRepository recipientRepository;
    private final TransporterProfileRepository transporterRepository;
    private final GuardianProfileRepository guardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public NoticeServiceImpl(NoticeRepository noticeRepository,
                             NoticeReactionRepository reactionRepository,
                             NoticeCommentRepository commentRepository,
                             NoticeViewRepository viewRepository,
                             NoticeRecipientRepository recipientRepository,
                             TransporterProfileRepository transporterRepository,
                             GuardianProfileRepository guardianRepository,
                             EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository) {
        this.noticeRepository = noticeRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.viewRepository = viewRepository;
        this.recipientRepository = recipientRepository;
        this.transporterRepository = transporterRepository;
        this.guardianRepository = guardianRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // ----- Transportador -----

    @Override
    @Transactional(readOnly = true)
    public List<NoticeResponse> listMyNotices(Long transporterUserId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        Instant now = Instant.now();
        return noticeRepository.findByTransporterIdOrderByCreatedAtDesc(transporter.getId()).stream()
                .sorted(urgentFirst())
                .map(n -> toTransporterResponse(n, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AudienceRecipientResponse> listAudience(Long transporterUserId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        return enrollmentRepository.findByTransporterId(transporter.getId()).stream()
                .filter(Enrollment::isActive)
                .map(e -> new AudienceRecipientResponse(
                        e.getDependent().getGuardian().getId(),
                        e.getDependent().getGuardian().getUser().getName(),
                        e.getDependent().getName(),
                        e.getDependent().getSchool(),
                        e.getDependent().getGuardian().getNeighborhood()))
                .toList();
    }

    @Override
    @Transactional
    public NoticeResponse create(Long transporterUserId, CreateNoticeRequest request) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        int hours = request.publishInHours() == null ? 0 : request.publishInHours();

        Notice notice = new Notice();
        notice.setTransporter(transporter);
        notice.setTitle(trimToNull(request.title()));
        notice.setMessage(request.message());
        notice.setPublishAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        notice.setAllowComments(request.allowComments() == null || request.allowComments());
        notice.setPriority(request.priority() == null ? NoticePriority.INFO : request.priority());

        // Define audienceAll + recipientsCount ANTES de salvar (coluna NOT NULL).
        List<Long> targets = resolveAudience(notice, transporter, request.recipientGuardianIds());
        Notice saved = noticeRepository.save(notice);
        persistRecipients(saved, targets);
        return toTransporterResponse(saved, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeResponse getMyNotice(Long transporterUserId, Long noticeId) {
        Notice notice = requireOwnNotice(transporterUserId, noticeId);
        return toTransporterResponse(notice, Instant.now());
    }

    @Override
    @Transactional
    public NoticeResponse update(Long transporterUserId, Long noticeId, UpdateNoticeRequest request) {
        Notice notice = requireOwnNotice(transporterUserId, noticeId);
        notice.setTitle(trimToNull(request.title()));
        notice.setMessage(request.message());
        if (request.priority() != null) {
            notice.setPriority(request.priority());
        }
        if (request.allowComments() != null) {
            notice.setAllowComments(request.allowComments());
        }
        if (request.publishInHours() != null) {
            // Reagenda a partir de agora (0 = publicar imediatamente).
            notice.setPublishAt(Instant.now().plus(request.publishInHours(), ChronoUnit.HOURS));
        }
        if (request.recipientGuardianIds() != null) {
            // Regrava o público (vazio = todos).
            recipientRepository.deleteByNoticeId(noticeId);
            List<Long> targets =
                    resolveAudience(notice, notice.getTransporter(), request.recipientGuardianIds());
            persistRecipients(notice, targets);
        }
        return toTransporterResponse(noticeRepository.save(notice), Instant.now());
    }

    @Override
    @Transactional
    public void delete(Long transporterUserId, Long noticeId) {
        Notice notice = requireOwnNotice(transporterUserId, noticeId);
        reactionRepository.deleteByNoticeId(noticeId);
        commentRepository.deleteByNoticeId(noticeId);
        viewRepository.deleteByNoticeId(noticeId);
        recipientRepository.deleteByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoticeCommentResponse> listCommentsAsTransporter(Long transporterUserId, Long noticeId) {
        requireOwnNotice(transporterUserId, noticeId);
        Long userId = resolveTransporter(transporterUserId).getUser().getId();
        return commentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId).stream()
                .map(c -> NoticeCommentResponse.from(c, userId))
                .toList();
    }

    @Override
    @Transactional
    public void moderateComment(Long transporterUserId, Long noticeId, Long commentId) {
        requireOwnNotice(transporterUserId, noticeId);
        NoticeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comentário", commentId));
        if (!comment.getNotice().getId().equals(noticeId)) {
            throw new BusinessException("Este comentário não pertence ao aviso informado.");
        }
        commentRepository.delete(comment);
    }

    // ----- Responsável -----

    @Override
    @Transactional(readOnly = true)
    public List<GuardianNoticeResponse> listForGuardian(Long guardianUserId) {
        GuardianProfile guardian = resolveGuardian(guardianUserId);
        return noticeRepository.findVisibleForGuardian(guardian.getId(), Instant.now()).stream()
                .sorted(urgentFirst())
                .map(n -> GuardianNoticeResponse.from(
                        n, reactionRepository.findByNoticeId(n.getId()),
                        guardianUserId, commentRepository.countByNoticeId(n.getId())))
                .toList();
    }

    @Override
    @Transactional
    public GuardianNoticeResponse getForGuardian(Long guardianUserId, Long noticeId) {
        Notice notice = requireVisibleNotice(guardianUserId, noticeId);
        markViewed(notice, guardianUserId);
        return guardianResponse(notice, guardianUserId);
    }

    @Override
    @Transactional
    public GuardianNoticeResponse setReaction(Long guardianUserId, Long noticeId, String emoji) {
        Notice notice = requireVisibleNotice(guardianUserId, noticeId);
        NoticeReaction reaction = reactionRepository.findByNoticeIdAndUserId(noticeId, guardianUserId)
                .orElseGet(() -> {
                    NoticeReaction created = new NoticeReaction();
                    created.setNotice(notice);
                    created.setUser(userRepository.getReferenceById(guardianUserId));
                    return created;
                });
        reaction.setEmoji(emoji);
        reactionRepository.save(reaction);
        return guardianResponse(notice, guardianUserId);
    }

    @Override
    @Transactional
    public GuardianNoticeResponse removeReaction(Long guardianUserId, Long noticeId) {
        Notice notice = requireVisibleNotice(guardianUserId, noticeId);
        reactionRepository.deleteByNoticeIdAndUserId(noticeId, guardianUserId);
        return guardianResponse(notice, guardianUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoticeCommentResponse> listCommentsAsGuardian(Long guardianUserId, Long noticeId) {
        requireVisibleNotice(guardianUserId, noticeId);
        return commentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId).stream()
                .map(c -> NoticeCommentResponse.from(c, guardianUserId))
                .toList();
    }

    @Override
    @Transactional
    public NoticeCommentResponse addComment(Long guardianUserId, Long noticeId, String text) {
        Notice notice = requireVisibleNotice(guardianUserId, noticeId);
        if (!notice.isAllowComments()) {
            throw new BusinessException("Este aviso não aceita comentários.");
        }
        NoticeComment comment = new NoticeComment();
        comment.setNotice(notice);
        comment.setAuthor(userRepository.getReferenceById(guardianUserId));
        comment.setText(text);
        return NoticeCommentResponse.from(commentRepository.save(comment), guardianUserId);
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long guardianUserId, Long noticeId, Long commentId) {
        requireVisibleNotice(guardianUserId, noticeId);
        NoticeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comentário", commentId));
        if (!comment.getNotice().getId().equals(noticeId)
                || !comment.getAuthor().getId().equals(guardianUserId)) {
            throw new BusinessException("Você só pode excluir os seus próprios comentários.");
        }
        commentRepository.delete(comment);
    }

    // ----- Helpers -----

    private NoticeResponse toTransporterResponse(Notice notice, Instant now) {
        return NoticeResponse.from(
                notice,
                reactionRepository.findByNoticeId(notice.getId()),
                now,
                (int) viewRepository.countByNoticeId(notice.getId()),
                commentRepository.countByNoticeId(notice.getId()));
    }

    private GuardianNoticeResponse guardianResponse(Notice notice, Long guardianUserId) {
        return GuardianNoticeResponse.from(
                notice,
                reactionRepository.findByNoticeId(notice.getId()),
                guardianUserId,
                commentRepository.countByNoticeId(notice.getId()));
    }

    private void markViewed(Notice notice, Long userId) {
        if (!viewRepository.existsByNoticeIdAndUserId(notice.getId(), userId)) {
            NoticeView view = new NoticeView();
            view.setNotice(notice);
            view.setUser(userRepository.getReferenceById(userId));
            viewRepository.save(view);
        }
    }

    /**
     * Define o público no aviso (audienceAll + recipientsCount) e devolve a lista
     * de responsáveis-alvo. Ids vazios/nulos ⇒ todos. Não persiste destinatários —
     * isso é feito por {@link #persistRecipients} APÓS o aviso ser salvo (para que
     * recipients_count nunca fique nulo e o NoticeRecipient referencie um id válido).
     */
    private List<Long> resolveAudience(Notice notice, TransporterProfile transporter, List<Long> requestedIds) {
        Set<Long> activeGuardians = activeGuardianIds(transporter.getId());
        List<Long> targets = (requestedIds == null) ? List.of()
                : requestedIds.stream().distinct().filter(activeGuardians::contains).toList();

        if (targets.isEmpty()) {
            notice.setAudienceAll(true);
            notice.setRecipientsCount(activeGuardians.size());
            return List.of();
        }
        notice.setAudienceAll(false);
        notice.setRecipientsCount(targets.size());
        return targets;
    }

    private void persistRecipients(Notice notice, List<Long> guardianIds) {
        for (Long guardianId : guardianIds) {
            NoticeRecipient recipient = new NoticeRecipient();
            recipient.setNotice(notice);
            recipient.setGuardian(guardianRepository.getReferenceById(guardianId));
            recipientRepository.save(recipient);
        }
    }

    private Set<Long> activeGuardianIds(Long transporterId) {
        return enrollmentRepository.findByTransporterId(transporterId).stream()
                .filter(Enrollment::isActive)
                .map(e -> e.getDependent().getGuardian().getId())
                .collect(Collectors.toSet());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Comparator<Notice> urgentFirst() {
        return Comparator.comparingInt(n -> n.resolvedPriority() == NoticePriority.URGENT ? 0 : 1);
    }

    private Notice requireOwnNotice(Long transporterUserId, Long noticeId) {
        TransporterProfile transporter = resolveTransporter(transporterUserId);
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Aviso", noticeId));
        if (!notice.getTransporter().getId().equals(transporter.getId())) {
            throw new BusinessException("Este aviso não pertence ao transportador autenticado.");
        }
        return notice;
    }

    private Notice requireVisibleNotice(Long guardianUserId, Long noticeId) {
        GuardianProfile guardian = resolveGuardian(guardianUserId);
        if (!noticeRepository.isVisibleToGuardian(noticeId, guardian.getId(), Instant.now())) {
            throw ResourceNotFoundException.of("Aviso", noticeId);
        }
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Aviso", noticeId));
    }

    private TransporterProfile resolveTransporter(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }

    private GuardianProfile resolveGuardian(Long userId) {
        return guardianRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de responsável", userId));
    }
}
