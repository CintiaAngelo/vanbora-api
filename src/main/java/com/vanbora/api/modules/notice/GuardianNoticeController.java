package com.vanbora.api.modules.notice;

import com.vanbora.api.modules.notice.dto.AddCommentRequest;
import com.vanbora.api.modules.notice.dto.GuardianNoticeResponse;
import com.vanbora.api.modules.notice.dto.NoticeCommentResponse;
import com.vanbora.api.modules.notice.dto.ReactionRequest;
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

/** Avisos vistos pelo responsável: reações por emoji e comentários. */
@RestController
@RequestMapping("/api/guardians/me/notices")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianNoticeController {

    private final NoticeService noticeService;
    private final CurrentUserProvider currentUserProvider;

    public GuardianNoticeController(NoticeService noticeService,
                                    CurrentUserProvider currentUserProvider) {
        this.noticeService = noticeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<GuardianNoticeResponse> list() {
        return noticeService.listForGuardian(currentUserProvider.requireUserId());
    }

    @GetMapping("/{id}")
    public GuardianNoticeResponse get(@PathVariable Long id) {
        return noticeService.getForGuardian(currentUserProvider.requireUserId(), id);
    }

    @PutMapping("/{id}/reaction")
    public GuardianNoticeResponse react(@PathVariable Long id,
                                        @Valid @RequestBody ReactionRequest request) {
        return noticeService.setReaction(currentUserProvider.requireUserId(), id, request.emoji());
    }

    @DeleteMapping("/{id}/reaction")
    public GuardianNoticeResponse removeReaction(@PathVariable Long id) {
        return noticeService.removeReaction(currentUserProvider.requireUserId(), id);
    }

    @GetMapping("/{id}/comments")
    public List<NoticeCommentResponse> comments(@PathVariable Long id) {
        return noticeService.listCommentsAsGuardian(currentUserProvider.requireUserId(), id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<NoticeCommentResponse> addComment(@PathVariable Long id,
                                                            @Valid @RequestBody AddCommentRequest request) {
        NoticeCommentResponse response =
                noticeService.addComment(currentUserProvider.requireUserId(), id, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        noticeService.deleteOwnComment(currentUserProvider.requireUserId(), id, commentId);
        return ResponseEntity.noContent().build();
    }
}
