package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.NoticeComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeCommentRepository extends JpaRepository<NoticeComment, Long> {

    List<NoticeComment> findByNoticeIdOrderByCreatedAtAsc(Long noticeId);

    long countByNoticeId(Long noticeId);

    @Transactional
    void deleteByNoticeId(Long noticeId);
}
