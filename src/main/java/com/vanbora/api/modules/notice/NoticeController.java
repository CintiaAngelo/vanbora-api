package com.vanbora.api.modules.notice;

import com.vanbora.api.modules.notice.dto.CreateNoticeRequest;
import com.vanbora.api.modules.notice.dto.NoticeCommentResponse;
import com.vanbora.api.modules.notice.dto.NoticeResponse;
import com.vanbora.api.modules.notice.dto.UpdateNoticeRequest;
import com.vanbora.api.modules.notice.service.NoticeService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gerenciamento de avisos do transportador (criar, editar, excluir, moderar). */
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

    @GetMapping("/{id}")
    public NoticeResponse get(@PathVariable Long id) {
        return noticeService.getMyNotice(currentUserProvider.requireUserId(), id);
    }

    @PostMapping
    public ResponseEntity<NoticeResponse> create(@Valid @RequestBody CreateNoticeRequest request) {
        NoticeResponse response = noticeService.create(currentUserProvider.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public NoticeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateNoticeRequest request) {
        return noticeService.update(currentUserProvider.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.delete(currentUserProvider.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public List<NoticeCommentResponse> comments(@PathVariable Long id) {
        return noticeService.listCommentsAsTransporter(currentUserProvider.requireUserId(), id);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> moderateComment(@PathVariable Long id, @PathVariable Long commentId) {
        noticeService.moderateComment(currentUserProvider.requireUserId(), id, commentId);
        return ResponseEntity.noContent().build();
    }
}
