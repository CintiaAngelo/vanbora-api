package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByTransporterIdOrderByCreatedAtDesc(Long transporterId);
}
