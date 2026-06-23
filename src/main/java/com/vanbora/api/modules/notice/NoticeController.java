package com.vanbora.api.modules.notice;

import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.service.NoticeService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gerenciamento de avisos do transportador. */
@RestController
@RequestMapping("/api/transporters/me/notices")
@PreAuthorize("hasRole('TRANSPORTER')")
public class NoticeController {

    private final NoticeService noticeService;
    private final CurrentUserProvider currentUserProvider;

    public NoticeController(NoticeService noticeService, CurrentUserProvider currentUserProvider) {
        this.noticeService = noticeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<NoticeResponse> list() {
        return noticeService.listMyNotices(currentUserProvider.requireUserId());
    }

    @PostMapping
    public ResponseEntity<NoticeResponse> create(@Valid @RequestBody CreateNoticeRequest request) {
        NoticeResponse response = noticeService.create(currentUserProvider.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
