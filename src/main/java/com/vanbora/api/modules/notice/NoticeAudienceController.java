package com.vanbora.api.modules.notice;

import com.vanbora.api.modules.notice.dto.AudienceRecipientResponse;
import com.vanbora.api.modules.notice.service.NoticeService;
import com.vanbora.api.security.CurrentUserProvider;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista de possíveis destinatários para segmentar avisos. */
@RestController
@RequestMapping("/api/transporters/me/audience")
@PreAuthorize("hasRole('TRANSPORTER')")
public class NoticeAudienceController {

    private final NoticeService noticeService;
    private final CurrentUserProvider currentUserProvider;

    public NoticeAudienceController(NoticeService noticeService,
                                    CurrentUserProvider currentUserProvider) {
        this.noticeService = noticeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<AudienceRecipientResponse> audience() {
        return noticeService.listAudience(currentUserProvider.requireUserId());
    }
}
