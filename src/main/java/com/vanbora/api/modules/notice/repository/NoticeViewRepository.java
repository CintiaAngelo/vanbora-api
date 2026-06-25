package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.NoticeView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeViewRepository extends JpaRepository<NoticeView, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    long countByNoticeId(Long noticeId);

    List<NoticeView> findByNoticeIdIn(List<Long> noticeIds);

    @Transactional
    void deleteByNoticeId(Long noticeId);
}
