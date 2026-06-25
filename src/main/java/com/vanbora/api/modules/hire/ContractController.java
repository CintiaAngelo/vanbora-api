package com.vanbora.api.modules.hire;

import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.modules.hire.dto.SignContractRequest;
import com.vanbora.api.modules.hire.service.ContractService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.enums.ContractStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contratos do responsável autenticado (listar, ver, assinar). */
@RestController
@RequestMapping("/api/guardians/me/contracts")
@PreAuthorize("hasRole('GUARDIAN')")
public class ContractController {

    private final ContractService contractService;
    private final CurrentUserProvider currentUserProvider;

    public ContractController(ContractService contractService,
                              CurrentUserProvider currentUserProvider) {
        this.contractService = contractService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<ContractResponse> list(@RequestParam(required = false) ContractStatus status) {
        return contractService.listForGuardian(currentUserProvider.requireUserId(), status);
    }

    @GetMapping("/{id}")
    public ContractResponse get(@PathVariable Long id) {
        return contractService.get(currentUserProvider.requireUserId(), id);
    }

    @PostMapping("/{id}/sign")
    public ContractResponse sign(@PathVariable Long id, @Valid @RequestBody SignContractRequest request) {
        return contractService.sign(currentUserProvider.requireUserId(), id, request.paymentMethodId());
    }
}
