package com.vanbora.api.modules.hire;

import com.vanbora.api.modules.hire.dto.ContractResponse;
import com.vanbora.api.modules.hire.service.ContractService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.shared.enums.ContractStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contratos do transportador autenticado (ex.: histórico de contratos cancelados). */
@RestController
@RequestMapping("/api/transporters/me/contracts")
@PreAuthorize("hasRole('TRANSPORTER')")
public class TransporterContractController {

    private final ContractService contractService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterContractController(ContractService contractService,
                                         CurrentUserProvider currentUserProvider) {
        this.contractService = contractService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<ContractResponse> list(@RequestParam(required = false) ContractStatus status) {
        return contractService.listForTransporter(currentUserProvider.requireUserId(), status);
    }
}
