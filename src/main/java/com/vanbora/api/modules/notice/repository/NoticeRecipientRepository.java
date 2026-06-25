package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.NoticeRecipient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {

    List<NoticeRecipient> findByNoticeId(Long noticeId);

    @Transactional
    void deleteByNoticeId(Long noticeId);
}
