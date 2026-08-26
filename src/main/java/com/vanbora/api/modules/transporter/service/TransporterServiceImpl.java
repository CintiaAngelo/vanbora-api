package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.PriceZone;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.CreateHelperRequest;
import com.vanbora.api.modules.transporter.dto.PriceZoneInput;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import com.vanbora.api.modules.transporter.dto.ReviewResponse;
import com.vanbora.api.modules.transporter.dto.ServiceAreaOptionsResponse;
import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import com.vanbora.api.modules.transporter.dto.UpdateHelperRequest;
import com.vanbora.api.modules.transporter.dto.UpdatePricingRequest;
import com.vanbora.api.modules.transporter.dto.UpdateServiceAreaRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleCharacteristicsRequest;
import com.vanbora.api.modules.transporter.dto.UpdateVehicleRequest;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.repository.ReviewRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.validation.DocumentValidations;
import com.vanbora.api.shared.validation.TextNormalization;
import com.vanbora.api.shared.storage.StorageService;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Implementação dos casos de uso do transportador. */
@Service
@Transactional(readOnly = true)
public class TransporterServiceImpl implements TransporterService {

    private final TransporterProfileRepository transporterRepository;
    private final ReviewRepository reviewRepository;
    private final HelperRepository helperRepository;
    private final StorageService storageService;

    public TransporterServiceImpl(
            TransporterProfileRepository transporterRepository,
            ReviewRepository reviewRepository,
            HelperRepository helperRepository,
            StorageService storageService) {
        this.transporterRepository = transporterRepository;
        this.reviewRepository = reviewRepository;
        this.helperRepository = helperRepository;
        this.storageService = storageService;
    }

    @Override
    public List<TransporterSummaryResponse> search(String school, String neighborhood, String sort, String dir) {
        String schoolRef = normalize(school);
        String neighborhoodRef = normalize(neighborhood);
        boolean hasRef = schoolRef != null || neighborhoodRef != null;

        // Traz todos e ordena "próximos" (que atendem a referência) primeiro; os
        // demais aparecem embaixo. Sem referência, ordena apenas por preço/avaliação.
        Comparator<TransporterProfile> byMetric = comparatorFor(sort, dir);
        Comparator<TransporterProfile> comparator = hasRef
                ? Comparator.comparingInt(
                        (TransporterProfile t) -> servesRef(t, schoolRef, neighborhoodRef) ? 0 : 1)
                        .thenComparing(byMetric)
                : byMetric;

        return transporterRepository.search(null, null).stream()
                .sorted(comparator)
                .map(t -> TransporterSummaryResponse.from(t, servesRef(t, schoolRef, neighborhoodRef)))
                .toList();
    }

    /** true se o transportador atende a escola OU o bairro de referência (contains, sem acento-sensível). */
    private boolean servesRef(TransporterProfile t, String school, String neighborhood) {
        boolean schoolMatch = school != null && t.getSchools().stream()
                .anyMatch(s -> s.toLowerCase().contains(school.toLowerCase()));
        boolean neighborhoodMatch = neighborhood != null && t.getNeighborhoods().stream()
                .anyMatch(n -> n.toLowerCase().contains(neighborhood.toLowerCase()));
        return schoolMatch || neighborhoodMatch;
    }

    @Override
    public ServiceAreaOptionsResponse getServiceAreaOptions() {
        return new ServiceAreaOptionsResponse(
                sortedPtBr(transporterRepository.findDistinctSchools()),
                sortedPtBr(transporterRepository.findDistinctNeighborhoods()));
    }

    @Override
    public TransporterDetailResponse getPublicProfile(Long transporterId) {
        TransporterProfile transporter = findOrThrow(transporterId);
        List<HelperResponse> helpers = helperRepository.findByTransporterId(transporterId).stream()
                .filter(Helper::isActive)
                .map(HelperResponse::from)
                .toList();
        List<ReviewResponse> reviews = reviewRepository
                .findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .map(ReviewResponse::from)
                .toList();
        return TransporterDetailResponse.from(transporter, helpers, reviews);
    }

    @Override
    public TransporterProfileResponse getMyProfile(Long userId) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        return profileResponse(transporter);
    }

    // ----- Perfil próprio -----

    @Override
    @Transactional
    public TransporterProfileResponse updateVehicle(Long userId, UpdateVehicleRequest request) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        if (request.cnh() != null && !request.cnh().isBlank()) {
            if (!DocumentValidations.isValidCnh(request.cnh())) {
                throw new BusinessException("CNH inválida. Informe os 11 dígitos do número de registro.");
            }
            transporter.setCnh(request.cnh().trim());
        }
        if (request.plate() != null && !request.plate().isBlank()) {
            if (!DocumentValidations.isValidPlate(request.plate())) {
                throw new BusinessException("Placa inválida. Use até 7 caracteres (letras e números).");
            }
            transporter.setPlate(request.plate().trim());
        }
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse updateServiceArea(Long userId, UpdateServiceAreaRequest request) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        if (request.schools() != null) {
            transporter.setSchools(cleanSet(request.schools()));
        }
        if (request.neighborhoods() != null) {
            transporter.setNeighborhoods(cleanSet(request.neighborhoods()));
        }
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse updatePricing(Long userId, UpdatePricingRequest request) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        if (request.monthlyFee() != null) {
            transporter.setBaseMonthlyFee(request.monthlyFee());
        }
        // Anual/parcelado nulos ⇒ plano não oferecido (limpa o valor).
        transporter.setAnnualPlanFee(request.annualFee());
        transporter.setInstallmentMonthlyFee(request.installmentMonthlyFee());
        transporter.setAcceptsProposals(request.acceptsProposals());
        syncPriceZones(transporter, request.zones());
        return profileResponse(transporterRepository.save(transporter));
    }

    /**
     * Sincroniza as zonas de preço com a lista enviada: cria as novas (id nulo),
     * atualiza as existentes e remove (orphanRemoval) as que sumiram. Zonas sem
     * nome ou sem mensalidade são ignoradas. Lista nula ⇒ não mexe nas zonas.
     */
    private void syncPriceZones(TransporterProfile transporter, List<PriceZoneInput> inputs) {
        if (inputs == null) {
            return;
        }
        List<PriceZone> current = transporter.getPriceZones();
        Map<Long, PriceZone> byId = current.stream()
                .filter(z -> z.getId() != null)
                .collect(Collectors.toMap(PriceZone::getId, z -> z));

        Set<Long> keepIds = new HashSet<>();
        for (PriceZoneInput in : inputs) {
            if (in.name() == null || in.name().isBlank() || in.monthlyFee() == null) {
                continue;
            }
            PriceZone zone = in.id() != null ? byId.get(in.id()) : null;
            if (zone == null) {
                zone = new PriceZone();
                zone.setTransporter(transporter);
                current.add(zone);
            }
            zone.setName(in.name().trim());
            zone.setSchool(StringUtils.hasText(in.school()) ? in.school().trim() : null);
            zone.setMonthlyFee(in.monthlyFee());
            zone.setAnnualFee(in.annualFee());
            zone.setInstallmentMonthlyFee(in.installmentMonthlyFee());
            zone.getNeighborhoods().clear();
            zone.getNeighborhoods().addAll(
                    cleanSet(in.neighborhoods() == null ? List.of() : in.neighborhoods()));
            if (zone.getId() != null) {
                keepIds.add(zone.getId());
            }
        }
        current.removeIf(z -> z.getId() != null && !keepIds.contains(z.getId()));
    }

    @Override
    @Transactional
    public TransporterProfileResponse updateContractTemplate(Long userId, String template) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        String cleaned = (template == null || template.isBlank()) ? null : template.trim();
        transporter.setContractTemplate(cleaned);
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse updateBio(Long userId, String bio) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        transporter.setBio((bio == null || bio.isBlank()) ? null : bio.trim());
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse setMyPhoto(Long userId, MultipartFile file) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        String previous = transporter.getPhotoUrl();
        transporter.setPhotoUrl(storageService.store(file, "transporters"));
        transporterRepository.save(transporter);
        storageService.delete(previous);
        return profileResponse(transporter);
    }

    // ----- Fotos e características do veículo -----

    private static final int MAX_VEHICLE_PHOTOS = 3;

    @Override
    @Transactional
    public TransporterProfileResponse addVehiclePhoto(Long userId, MultipartFile file) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        List<String> photos = transporter.getVehiclePhotoUrls();
        if (photos.size() >= MAX_VEHICLE_PHOTOS) {
            throw new BusinessException("Limite de " + MAX_VEHICLE_PHOTOS + " fotos do veículo atingido.");
        }
        photos.add(storageService.store(file, "vehicles"));
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse replaceVehiclePhoto(Long userId, int index, MultipartFile file) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        List<String> photos = transporter.getVehiclePhotoUrls();
        if (index < 0 || index >= photos.size()) {
            throw new BusinessException("Posição de foto inválida.");
        }
        String previous = photos.get(index);
        photos.set(index, storageService.store(file, "vehicles"));
        transporterRepository.save(transporter);
        storageService.delete(previous);
        return profileResponse(transporter);
    }

    @Override
    @Transactional
    public TransporterProfileResponse deleteVehiclePhoto(Long userId, int index) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        List<String> photos = transporter.getVehiclePhotoUrls();
        if (index < 0 || index >= photos.size()) {
            throw new BusinessException("Posição de foto inválida.");
        }
        String removed = photos.remove(index);
        transporterRepository.save(transporter);
        storageService.delete(removed);
        return profileResponse(transporter);
    }

    @Override
    @Transactional
    public TransporterProfileResponse reorderVehiclePhotos(Long userId, List<Integer> newOrder) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        List<String> photos = transporter.getVehiclePhotoUrls();
        if (newOrder == null || newOrder.size() != photos.size()
                || !new HashSet<>(newOrder).equals(
                        IntStream.range(0, photos.size()).boxed().collect(Collectors.toSet()))) {
            throw new BusinessException("Ordem de fotos inválida.");
        }
        List<String> reordered = newOrder.stream().map(photos::get).collect(Collectors.toList());
        photos.clear();
        photos.addAll(reordered);
        return profileResponse(transporterRepository.save(transporter));
    }

    @Override
    @Transactional
    public TransporterProfileResponse updateVehicleCharacteristics(
            Long userId, UpdateVehicleCharacteristicsRequest request) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        transporter.setVehicleCharacteristics(request.characteristics() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(request.characteristics()));
        transporter.setVehicleAccessibilityFeatures(request.accessibilityFeatures() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(request.accessibilityFeatures()));
        return profileResponse(transporterRepository.save(transporter));
    }

    // ----- Ajudantes -----

    @Override
    public List<HelperResponse> listMyHelpers(Long userId) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        return helperRepository.findByTransporterId(transporter.getId()).stream()
                .map(HelperResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public HelperResponse addHelper(Long userId, CreateHelperRequest request) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        Helper helper = new Helper();
        helper.setTransporter(transporter);
        helper.setName(request.name().trim());
        helper.setRole(request.role().trim());
        helper.setActive(true);
        return HelperResponse.from(helperRepository.save(helper));
    }

    @Override
    @Transactional
    public HelperResponse updateHelper(Long userId, Long helperId, UpdateHelperRequest request) {
        Helper helper = findOwnedHelper(userId, helperId);
        helper.setName(request.name().trim());
        helper.setRole(request.role().trim());
        if (request.active() != null) {
            helper.setActive(request.active());
        }
        return HelperResponse.from(helperRepository.save(helper));
    }

    @Override
    @Transactional
    public HelperResponse setHelperPhoto(Long userId, Long helperId, MultipartFile file) {
        Helper helper = findOwnedHelper(userId, helperId);
        String previous = helper.getPhotoUrl();
        helper.setPhotoUrl(storageService.store(file, "helpers"));
        helperRepository.save(helper);
        storageService.delete(previous);
        return HelperResponse.from(helper);
    }

    @Override
    @Transactional
    public void deleteHelper(Long userId, Long helperId) {
        Helper helper = findOwnedHelper(userId, helperId);
        String photo = helper.getPhotoUrl();
        helperRepository.delete(helper);
        storageService.delete(photo);
    }

    // ----- Helpers internos -----

    private TransporterProfileResponse profileResponse(TransporterProfile transporter) {
        List<HelperResponse> helpers = helperRepository.findByTransporterId(transporter.getId()).stream()
                .map(HelperResponse::from)
                .toList();
        return TransporterProfileResponse.from(transporter, helpers);
    }

    private Helper findOwnedHelper(Long userId, Long helperId) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        Helper helper = helperRepository.findById(helperId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ajudante", helperId));
        if (!helper.getTransporter().getId().equals(transporter.getId())) {
            throw new BusinessException("Este ajudante não pertence ao transportador autenticado.");
        }
        return helper;
    }

    /** Ordena alfabeticamente respeitando acentuação do português (a, á, b, ç, ...). */
    private List<String> sortedPtBr(List<String> values) {
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        return values.stream()
                .filter(StringUtils::hasText)
                .sorted(collator)
                .toList();
    }

    /** Limpa, colapsa espaços e padroniza a capitalização (preserva acentos) — evita duplicados por caixa. */
    private Set<String> cleanSet(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(TextNormalization.titleCase(value));
            }
        }
        return result;
    }

    /**
     * Ordena por preço ou avaliação. {@code dir="desc"} inverte: 1º clique (asc) traz
     * mais baratos / pior avaliados; 2º clique (desc) traz mais caros / melhor avaliados.
     */
    private Comparator<TransporterProfile> comparatorFor(String sort, String dir) {
        Comparator<TransporterProfile> base = "rating".equalsIgnoreCase(sort)
                ? Comparator.comparing(TransporterProfile::getRatingAvg,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(TransporterProfile::getBaseMonthlyFee,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        return "desc".equalsIgnoreCase(dir) ? base.reversed() : base;
    }

    private TransporterProfile findOrThrow(Long id) {
        return transporterRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transportador", id));
    }

    private TransporterProfile findByUserOrThrow(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador do usuário", userId));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
