package com.vanbora.api.modules.school.service;

import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.dto.CreateSchoolRequest;
import com.vanbora.api.modules.school.dto.SchoolResponse;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.geo.GeocodingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Implementação do catálogo de escolas (busca + cadastro geocodificado). */
@Service
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final GeocodingService geocodingService;

    public SchoolServiceImpl(SchoolRepository schoolRepository, GeocodingService geocodingService) {
        this.schoolRepository = schoolRepository;
        this.geocodingService = geocodingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolResponse> search(String query, Pageable pageable) {
        Page<School> page = StringUtils.hasText(query)
                ? schoolRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim(), pageable)
                : schoolRepository.findAllByOrderByNameAsc(pageable);
        return page.map(SchoolResponse::from);
    }

    @Override
    @Transactional
    public SchoolResponse register(CreateSchoolRequest request) {
        if (!StringUtils.hasText(request.name())) {
            throw new BusinessException("Informe o nome da escola.");
        }
        String name = request.name().trim();
        String cep = request.cep() == null ? null : request.cep().trim();

        // Reaproveita se já houver a mesma escola (mesmo nome + CEP).
        if (cep != null) {
            var existing = schoolRepository.findFirstByNameIgnoreCaseAndCep(name, cep);
            if (existing.isPresent()) {
                return SchoolResponse.from(existing.get());
            }
        }

        School school = new School();
        school.setName(name);
        school.setCep(cep);
        school.setStreet(trim(request.street()));
        school.setNumber(trim(request.number()));
        school.setNeighborhood(trim(request.neighborhood()));
        school.setCity(trim(request.city()));
        school.setUf(trim(request.uf()));
        geocodeInto(school);
        return SchoolResponse.from(schoolRepository.save(school));
    }

    /** Geocodifica pelo endereço (CEP), com fallback para nome + cidade. */
    private void geocodeInto(School school) {
        String byAddress = buildAddressQuery(school);
        var coord = geocodingService.geocode(byAddress);
        if (coord.isEmpty()) {
            coord = geocodingService.geocode(school.getName() + ", " + safe(school.getCity()) + ", Brasil");
        }
        coord.ifPresent(c -> {
            school.setLatitude(c[0]);
            school.setLongitude(c[1]);
        });
    }

    private String buildAddressQuery(School s) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, s.getStreet());
        if (StringUtils.hasText(s.getNumber())) {
            sb.append(sb.length() > 0 ? ", " : "").append(s.getNumber());
        }
        appendPart(sb, s.getNeighborhood());
        appendPart(sb, s.getCity());
        appendPart(sb, s.getUf());
        if (StringUtils.hasText(s.getCep())) {
            sb.append(sb.length() > 0 ? ", " : "").append(s.getCep());
        }
        sb.append(sb.length() > 0 ? ", Brasil" : "");
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String part) {
        if (StringUtils.hasText(part)) {
            sb.append(sb.length() > 0 ? ", " : "").append(part.trim());
        }
    }

    private String trim(String v) {
        return v == null ? null : v.trim();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
