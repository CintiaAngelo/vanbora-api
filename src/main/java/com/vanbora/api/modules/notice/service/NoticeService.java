package com.vanbora.api.modules.notice.service;

import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import java.util.List;

/** Casos de uso de avisos do transportador. */
public interface NoticeService {

    List<NoticeResponse> listMyNotices(Long transporterUserId);

    NoticeResponse create(Long transporterUserId, CreateNoticeRequest request);
}
