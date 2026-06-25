package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.NoticeReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeReactionRepository extends JpaRepository<NoticeReaction, Long> {

    List<NoticeReaction> findByNoticeId(Long noticeId);

    List<NoticeReaction> findByNoticeIdIn(List<Long> noticeIds);

    Optional<NoticeReaction> findByNoticeIdAndUserId(Long noticeId, Long userId);

    @Transactional
    void deleteByNoticeIdAndUserId(Long noticeId, Long userId);

    @Transactional
    void deleteByNoticeId(Long noticeId);
}
