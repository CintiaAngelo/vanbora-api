package com.vanbora.api.modules.trip.repository;

import com.vanbora.api.modules.trip.domain.SafetyChecklistPhoto;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyChecklistPhotoRepository extends JpaRepository<SafetyChecklistPhoto, Long> {

    /** Fotos com arquivo ainda guardado cujo prazo de retenção já venceu. */
    @Query("""
            select p from SafetyChecklistPhoto p
            where p.purgedAt is null and p.checklist.photosPurgeAt is not null
              and p.checklist.photosPurgeAt <= :now
            """)
    List<SafetyChecklistPhoto> findExpired(@Param("now") Instant now);
}
