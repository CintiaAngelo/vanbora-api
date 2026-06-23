package com.vanbora.api.modules.guardian.repository;

import com.vanbora.api.modules.guardian.domain.Dependent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependentRepository extends JpaRepository<Dependent, Long> {

    List<Dependent> findByGuardianId(Long guardianId);
}
