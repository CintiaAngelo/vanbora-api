package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.HelperResponse;
import com.vanbora.api.modules.transporter.dto.ReviewResponse;
import com.vanbora.api.modules.transporter.dto.TransporterDetailResponse;
import com.vanbora.api.modules.transporter.dto.TransporterProfileResponse;
import com.vanbora.api.modules.transporter.dto.TransporterSummaryResponse;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.repository.ReviewRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Implementação dos casos de uso do transportador. */
@Service
@Transactional(readOnly = true)
public class TransporterServiceImpl implements TransporterService {

    private final TransporterProfileRepository transporterRepository;
    private final ReviewRepository reviewRepository;
    private final HelperRepository helperRepository;

    public TransporterServiceImpl(
            TransporterProfileRepository transporterRepository,
            ReviewRepository reviewRepository,
            HelperRepository helperRepository) {
        this.transporterRepository = transporterRepository;
        this.reviewRepository = reviewRepository;
        this.helperRepository = helperRepository;
    }

    @Override
    public List<TransporterSummaryResponse> search(String school, String neighborhood, String sort) {
        String schoolFilter = normalize(school);
        String neighborhoodFilter = normalize(neighborhood);

        return transporterRepository.search(schoolFilter, neighborhoodFilter).stream()
                .sorted(comparatorFor(sort))
                .map(TransporterSummaryResponse::from)
                .toList();
    }

    @Override
    public TransporterDetailResponse getPublicProfile(Long transporterId) {
        TransporterProfile transporter = findOrThrow(transporterId);
        List<ReviewResponse> reviews = reviewRepository
                .findByTransporterIdOrderByCreatedAtDesc(transporterId).stream()
                .map(ReviewResponse::from)
                .toList();
        return TransporterDetailResponse.from(transporter, reviews);
    }

    @Override
    public TransporterProfileResponse getMyProfile(Long userId) {
        TransporterProfile transporter = findByUserOrThrow(userId);
        List<HelperResponse> helpers = helperRepository.findByTransporterId(transporter.getId()).stream()
                .map(HelperResponse::from)
                .toList();
        return TransporterProfileResponse.from(transporter, helpers);
    }

    private Comparator<TransporterProfile> comparatorFor(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return Comparator.comparing(TransporterProfile::getRatingAvg).reversed();
        }
        return Comparator.comparing(TransporterProfile::getBaseMonthlyFee);
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
