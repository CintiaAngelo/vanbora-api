package com.vanbora.api.modules.transporter;

import com.vanbora.api.modules.transporter.dto.ServiceAreaOptionsResponse;
import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import com.vanbora.api.modules.transporter.dto.UpdateContractTemplateRequest;
import com.vanbora.api.modules.transporter.dto.UpdatePricingRequest;
import com.vanbora.api.modules.transporter.dto.UpdateServiceAreaRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleCharacteristicsRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleRequest;
import com.vanbora.api.modules.transporter.dto.VehiclePhotoOrderRequest;
import com.vanbora.api.modules.transporter.service.TransporterService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Endpoints de busca, perfil público e perfil próprio do transportador. */
@RestController
@RequestMapping("/api/transporters")
public class TransporterController {

    private final TransporterService transporterService;
    private final CurrentUserProvider currentUserProvider;

    public TransporterController(TransporterService transporterService,
                                 CurrentUserProvider currentUserProvider) {
        this.transporterService = transporterService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Busca transportadores: os que atendem a escola/bairro informados vêm primeiro
     * ("perto de você"), os demais embaixo. Ordena por preço (padrão) ou avaliação,
     * na direção asc (padrão) ou desc.
     */
    @GetMapping
    public List<TransporterSummaryResponse> search(
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false, defaultValue = "price") String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir) {
        return transporterService.search(school, neighborhood, sort, dir);
    }

    /** Escolas e bairros já cadastrados, em ordem alfabética, para os filtros da busca. */
    @GetMapping("/filters")
    public ServiceAreaOptionsResponse filters() {
        return transporterService.getServiceAreaOptions();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse myProfile() {
        return transporterService.getMyProfile(currentUserProvider.requireUserId());
    }

    /** Edita os dados do veículo (CNH, placa, capacidade). */
    @PutMapping("/me")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updateVehicle(@Valid @RequestBody UpdateVehicleRequest request) {
        return transporterService.updateVehicle(currentUserProvider.requireUserId(), request);
    }

    /** Edita a área de atendimento (escolas e bairros). */
    @PutMapping("/me/service-area")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updateServiceArea(@RequestBody UpdateServiceAreaRequest request) {
        return transporterService.updateServiceArea(currentUserProvider.requireUserId(), request);
    }

    /** Configura o valor de tabela e se aceita propostas de valor. */
    @PutMapping("/me/pricing")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updatePricing(@Valid @RequestBody UpdatePricingRequest request) {
        return transporterService.updatePricing(currentUserProvider.requireUserId(), request);
    }

    /** Define/limpa o modelo de contrato (texto) exibido ao responsável na assinatura. */
    @PutMapping("/me/contract-template")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updateContractTemplate(
            @RequestBody UpdateContractTemplateRequest request) {
        return transporterService.updateContractTemplate(
                currentUserProvider.requireUserId(), request.template());
    }

    /** Atualiza a descrição/biografia do transportador. */
    @PutMapping("/me/bio")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updateBio(
            @jakarta.validation.Valid @RequestBody com.vanbora.api.modules.transporter.dto.UpdateBioRequest request) {
        return transporterService.updateBio(currentUserProvider.requireUserId(), request.bio());
    }

    /** Define/atualiza a foto do transportador. */
    @PostMapping("/me/photo")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        return transporterService.setMyPhoto(currentUserProvider.requireUserId(), file);
    }

    /** Adiciona uma foto do veículo (máx. 3; a primeira é a principal). */
    @PostMapping("/me/vehicle-photos")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse addVehiclePhoto(@RequestParam("file") MultipartFile file) {
        return transporterService.addVehiclePhoto(currentUserProvider.requireUserId(), file);
    }

    /** Substitui a foto do veículo na posição informada (0 = principal). */
    @PutMapping("/me/vehicle-photos/{index}")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse replaceVehiclePhoto(
            @PathVariable int index, @RequestParam("file") MultipartFile file) {
        return transporterService.replaceVehiclePhoto(currentUserProvider.requireUserId(), index, file);
    }

    /** Remove a foto do veículo na posição informada. */
    @DeleteMapping("/me/vehicle-photos/{index}")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse deleteVehiclePhoto(@PathVariable int index) {
        return transporterService.deleteVehiclePhoto(currentUserProvider.requireUserId(), index);
    }

    /** Reordena as fotos do veículo. */
    @PutMapping("/me/vehicle-photos/order")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse reorderVehiclePhotos(@RequestBody VehiclePhotoOrderRequest request) {
        return transporterService.reorderVehiclePhotos(currentUserProvider.requireUserId(), request.order());
    }

    /** Substitui as características e a acessibilidade do veículo. */
    @PutMapping("/me/vehicle-characteristics")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public TransporterProfileResponse updateVehicleCharacteristics(
            @RequestBody UpdateVehicleCharacteristicsRequest request) {
        return transporterService.updateVehicleCharacteristics(currentUserProvider.requireUserId(), request);
    }

    @GetMapping("/{id}")
    public TransporterDetailResponse publicProfile(@PathVariable Long id) {
        return transporterService.getPublicProfile(id);
    }
}
