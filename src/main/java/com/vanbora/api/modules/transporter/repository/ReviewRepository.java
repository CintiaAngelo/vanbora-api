package com.vanbora.api.modules.transporter.repository;

import com.vanbora.api.modules.transporter.domain.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTransporterIdOrderByCreatedAtDesc(Long transporterId);
}
