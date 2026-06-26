package com.vanbora.api.modules.school;

import com.vanbora.api.modules.school.dto.CreateSchoolRequest;
import com.vanbora.api.modules.school.dto.SchoolResponse;
import com.vanbora.api.modules.school.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de escolas: busca (qualquer usuário) e cadastro (transportador). */
@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private static final int MAX_SIZE = 50;

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    /** Busca paginada por nome (query opcional). Acessível a responsáveis e transportadores. */
    @GetMapping
    public Page<SchoolResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return schoolService.search(query, PageRequest.of(Math.max(page, 0), safeSize));
    }

    /** Cadastra uma escola no catálogo (por CEP). Apenas transportadores. */
    @PostMapping
    @PreAuthorize("hasRole('TRANSPORTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse register(@Valid @RequestBody CreateSchoolRequest request) {
        return schoolService.register(request);
    }
}
