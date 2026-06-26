package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.dto.CreateHelperRequest;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import com.vanbora.api.modules.transporter.dto.ServiceAreaOptionsResponse;
import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import com.vanbora.api.modules.transporter.dto.UpdateHelperRequest;
import com.vanbora.api.modules.transporter.dto.UpdatePricingRequest;
import com.vanbora.api.modules.transporter.dto.UpdateServiceAreaRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Casos de uso relacionados ao transportador (busca pública e perfil próprio). */
public interface TransporterService {

    List<TransporterSummaryResponse> search(String school, String neighborhood, String sort);

    /** Escolas e bairros já cadastrados (para os autocompletes da busca), em ordem alfabética. */
    ServiceAreaOptionsResponse getServiceAreaOptions();

    TransporterDetailResponse getPublicProfile(Long transporterId);

    TransporterProfileResponse getMyProfile(Long userId);

    // ----- Perfil próprio -----

    TransporterProfileResponse updateVehicle(Long userId, UpdateVehicleRequest request);

    TransporterProfileResponse updateServiceArea(Long userId, UpdateServiceAreaRequest request);

    TransporterProfileResponse updatePricing(Long userId, UpdatePricingRequest request);

    /** Define/limpa o modelo de contrato (texto) usado na assinatura do responsável. */
    TransporterProfileResponse updateContractTemplate(Long userId, String template);

    TransporterProfileResponse setMyPhoto(Long userId, MultipartFile file);

    // ----- Ajudantes -----

    List<HelperResponse> listMyHelpers(Long userId);

    HelperResponse addHelper(Long userId, CreateHelperRequest request);

    HelperResponse updateHelper(Long userId, Long helperId, UpdateHelperRequest request);

    HelperResponse setHelperPhoto(Long userId, Long helperId, MultipartFile file);

    void deleteHelper(Long userId, Long helperId);
}
