package com.vanbora.api.modules.school.repository;

import com.vanbora.api.modules.school.domain.School;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Page<School> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    Page<School> findAllByOrderByNameAsc(Pageable pageable);

    /** Para evitar duplicatas quando dois transportadores cadastram a mesma escola. */
    Optional<School> findFirstByNameIgnoreCaseAndCep(String name, String cep);

    boolean existsByNameIgnoreCase(String name);

    /** Escola vinculada à conta (perfil SCHOOL) do usuário autenticado no painel de gestão. */
    Optional<School> findByUserId(Long userId);
}
