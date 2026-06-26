package com.vanbora.api.modules.transporter.repository;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransporterProfileRepository extends JpaRepository<TransporterProfile, Long> {

    Optional<TransporterProfile> findByUserId(Long userId);

    /**
     * Busca transportadores que atendam a escola e/ou bairro informados.
     * Filtros nulos são ignorados (busca ampla).
     */
    @Query("""
            select distinct t from TransporterProfile t
            left join t.schools s
            left join t.neighborhoods n
            where (:school is null or lower(s) like lower(concat('%', :school, '%')))
              and (:neighborhood is null or lower(n) like lower(concat('%', :neighborhood, '%')))
            """)
    List<TransporterProfile> search(@Param("school") String school,
                                    @Param("neighborhood") String neighborhood);

    /** Escolas distintas atendidas por algum transportador (para os filtros de busca). */
    @Query("select distinct s from TransporterProfile t join t.schools s")
    List<String> findDistinctSchools();

    /** Bairros distintos atendidos por algum transportador (para os filtros de busca). */
    @Query("select distinct n from TransporterProfile t join t.neighborhoods n")
    List<String> findDistinctNeighborhoods();
}
