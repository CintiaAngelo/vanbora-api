package com.vanbora.api.modules.notice.repository;

import com.vanbora.api.modules.notice.domain.Notice;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByTransporterIdOrderByCreatedAtDesc(Long transporterId);

    /**
     * Avisos publicados dos transportadores com matrícula ativa do responsável,
     * respeitando a segmentação: ou o aviso é para todos (audienceAll != false),
     * ou o responsável está na lista de destinatários (NoticeRecipient).
     */
    @Query("""
            select n from Notice n
            where n.publishAt <= :now
              and n.transporter.id in (
                  select e.transporter.id from Enrollment e
                  where e.dependent.guardian.id = :guardianId and e.active = true)
              and (n.audienceAll is null or n.audienceAll = true
                   or exists (select r from NoticeRecipient r
                              where r.notice = n and r.guardian.id = :guardianId))
            order by n.publishAt desc
            """)
    List<Notice> findVisibleForGuardian(@Param("guardianId") Long guardianId,
                                        @Param("now") Instant now);

    /** Indica se um aviso específico está visível ao responsável (publicado + segmentação). */
    @Query("""
            select (count(n) > 0) from Notice n
            where n.id = :noticeId and n.publishAt <= :now
              and n.transporter.id in (
                  select e.transporter.id from Enrollment e
                  where e.dependent.guardian.id = :guardianId and e.active = true)
              and (n.audienceAll is null or n.audienceAll = true
                   or exists (select r from NoticeRecipient r
                              where r.notice = n and r.guardian.id = :guardianId))
            """)
    boolean isVisibleToGuardian(@Param("noticeId") Long noticeId,
                                @Param("guardianId") Long guardianId,
                                @Param("now") Instant now);
}
