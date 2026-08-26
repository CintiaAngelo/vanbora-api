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
import com.vanbora.api.modules.transporter.dto.UpdateVehicleCharacteristicsRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Casos de uso relacionados ao transportador (busca pública e perfil próprio). */
public interface TransporterService {

    List<TransporterSummaryResponse> search(String school, String neighborhood, String sort, String dir);

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

    /** Atualiza a descrição/biografia do transportador. */
    TransporterProfileResponse updateBio(Long userId, String bio);

    TransporterProfileResponse setMyPhoto(Long userId, MultipartFile file);

    // ----- Fotos e características do veículo -----

    /** Adiciona uma foto do veículo ao final da lista (máx. 3). */
    TransporterProfileResponse addVehiclePhoto(Long userId, MultipartFile file);

    /** Substitui a foto na posição informada, mantendo a ordem. */
    TransporterProfileResponse replaceVehiclePhoto(Long userId, int index, MultipartFile file);

    /** Remove a foto na posição informada; as seguintes sobem uma posição. */
    TransporterProfileResponse deleteVehiclePhoto(Long userId, int index);

    /** Reordena as fotos do veículo (índice 0 passa a ser a principal). */
    TransporterProfileResponse reorderVehiclePhotos(Long userId, List<Integer> newOrder);

    /** Substitui por completo as características e a acessibilidade do veículo. */
    TransporterProfileResponse updateVehicleCharacteristics(Long userId, UpdateVehicleCharacteristicsRequest request);

    // ----- Ajudantes -----

    List<HelperResponse> listMyHelpers(Long userId);

    HelperResponse addHelper(Long userId, CreateHelperRequest request);

    HelperResponse updateHelper(Long userId, Long helperId, UpdateHelperRequest request);

    HelperResponse setHelperPhoto(Long userId, Long helperId, MultipartFile file);

    void deleteHelper(Long userId, Long helperId);
}
