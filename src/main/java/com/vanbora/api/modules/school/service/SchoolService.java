package com.vanbora.api.modules.school.service;

import com.vanbora.api.modules.school.dto.CreateSchoolRequest;
import com.vanbora.api.modules.school.dto.SchoolResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Catálogo global de escolas. */
public interface SchoolService {

    /** Busca paginada por nome (query vazia = todas), ordenada por nome. */
    Page<SchoolResponse> search(String query, Pageable pageable);

    /** Cadastra (ou reaproveita, se já existir) uma escola e a geocodifica. */
    SchoolResponse register(CreateSchoolRequest request);
}
