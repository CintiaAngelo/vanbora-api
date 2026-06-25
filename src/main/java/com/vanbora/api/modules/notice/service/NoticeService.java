package com.vanbora.api.modules.notice.service;

import com.vanbora.api.modules.notice.dto.AudienceRecipientResponse;
import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.GuardianNoticeResponse;
import com.vanbora.api.modules.notice.dto.NoticeCommentResponse;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.dto.UpdateNoticeRequest;
import java.util.List;

/** Casos de uso do mural de avisos (transportador e responsável). */
public interface NoticeService {

    // ----- Transportador -----
    List<NoticeResponse> listMyNotices(Long transporterUserId);

    /** Possíveis destinatários (responsáveis com matrícula ativa) para segmentar. */
    List<AudienceRecipientResponse> listAudience(Long transporterUserId);

    NoticeResponse create(Long transporterUserId, CreateNoticeRequest request);

    NoticeResponse getMyNotice(Long transporterUserId, Long noticeId);

    NoticeResponse update(Long transporterUserId, Long noticeId, UpdateNoticeRequest request);

    void delete(Long transporterUserId, Long noticeId);

    List<NoticeCommentResponse> listCommentsAsTransporter(Long transporterUserId, Long noticeId);

    void moderateComment(Long transporterUserId, Long noticeId, Long commentId);

    // ----- Responsável -----
    List<GuardianNoticeResponse> listForGuardian(Long guardianUserId);

    GuardianNoticeResponse getForGuardian(Long guardianUserId, Long noticeId);

    GuardianNoticeResponse setReaction(Long guardianUserId, Long noticeId, String emoji);

    GuardianNoticeResponse removeReaction(Long guardianUserId, Long noticeId);

    List<NoticeCommentResponse> listCommentsAsGuardian(Long guardianUserId, Long noticeId);

    NoticeCommentResponse addComment(Long guardianUserId, Long noticeId, String text);

    void deleteOwnComment(Long guardianUserId, Long noticeId, Long commentId);
}
