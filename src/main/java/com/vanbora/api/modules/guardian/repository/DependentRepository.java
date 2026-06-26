package com.vanbora.api.modules.guardian.repository;

import com.vanbora.api.modules.guardian.domain.Dependent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DependentRepository extends JpaRepository<Dependent, Long> {

    Optional<Dependent> findByIdAndGuardianId(Long id, Long guardianId);

    /** Dependentes NÃO arquivados do responsável (null ⇒ ativo). */
    @Query("select d from Dependent d where d.guardian.id = :guardianId "
            + "and (d.archived is null or d.archived = false) order by d.id asc")
    List<Dependent> findActiveByGuardian(@Param("guardianId") Long guardianId);

    @Query("select d from Dependent d where d.id = :id and d.guardian.id = :guardianId "
            + "and (d.archived is null or d.archived = false)")
    Optional<Dependent> findActiveByIdAndGuardian(@Param("id") Long id, @Param("guardianId") Long guardianId);
}
