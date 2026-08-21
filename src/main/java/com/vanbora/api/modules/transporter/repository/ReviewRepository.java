package com.vanbora.api.modules.transporter.repository;

import com.vanbora.api.modules.transporter.domain.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTransporterIdOrderByCreatedAtDesc(Long transporterId);

    List<Review> findByTransporterId(Long transporterId);

    /** Última avaliação deste responsável para este transportador (p/ pesquisa de 3 meses). */
    Optional<Review> findFirstByTransporterIdAndGuardianIdOrderByCreatedAtDesc(
            Long transporterId, Long guardianId);

    /** Avaliações recentes de um conjunto de transportadores (atividade recente do painel escolar). */
    List<Review> findByTransporterIdInOrderByCreatedAtDesc(List<Long> transporterIds);
}
