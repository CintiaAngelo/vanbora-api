package com.vanbora.api.modules.transporter.repository;

import com.vanbora.api.modules.transporter.domain.Helper;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelperRepository extends JpaRepository<Helper, Long> {

    List<Helper> findByTransporterId(Long transporterId);
}
