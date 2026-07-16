package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.finance.domain.DailyDistance;
import com.vanbora.api.modules.finance.repository.DailyDistanceRepository;
import com.vanbora.api.modules.transporter.domain.LocationShareWindow;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.dto.LocationSharingResponse;
import com.vanbora.api.modules.transporter.dto.LocationWindowInput;
import com.vanbora.api.modules.transporter.dto.TransporterLocationResponse;
import com.vanbora.api.modules.transporter.dto.UpdateLocationRequest;
import com.vanbora.api.modules.transporter.dto.UpdateLocationSharingRequest;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.util.GeoUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransporterLocationServiceImpl implements TransporterLocationService {

    // Filtros para o km derivado do GPS: ignora ruído (<20 m) e saltos (>5 km),
    // e só acumula se o ping anterior for recente (evita somar pausas longas).
    private static final double MIN_SEGMENT_KM = 0.02;
    private static final double MAX_SEGMENT_KM = 5.0;
    private static final Duration MAX_GAP = Duration.ofMinutes(10);

    private final TransporterProfileRepository transporterRepository;
    private final DailyDistanceRepository dailyDistanceRepository;

    public TransporterLocationServiceImpl(TransporterProfileRepository transporterRepository,
                                          DailyDistanceRepository dailyDistanceRepository) {
        this.transporterRepository = transporterRepository;
        this.dailyDistanceRepository = dailyDistanceRepository;
    }

    @Override
    @Transactional
    public TransporterLocationResponse updateLocation(Long userId, UpdateLocationRequest request) {
        TransporterProfile transporter = transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));

        accumulateDistance(transporter, request.latitude(), request.longitude());

        transporter.setCurrentLatitude(request.latitude());
        transporter.setCurrentLongitude(request.longitude());
        transporter.setLocationUpdatedAt(Instant.now());
        transporterRepository.save(transporter);

        return TransporterLocationResponse.from(transporter);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationSharingResponse getSharing(Long userId) {
        return LocationSharingResponse.from(requireTransporter(userId));
    }

    @Override
    @Transactional
    public LocationSharingResponse updateSharing(Long userId, UpdateLocationSharingRequest request) {
        TransporterProfile transporter = requireTransporter(userId);
        transporter.setLocationSharingEnabled(request.enabled());

        // Substitui todas as janelas pela lista enviada (orphanRemoval cuida das removidas).
        transporter.getLocationShareWindows().clear();
        List<LocationWindowInput> windows = request.windows();
        if (windows != null) {
            for (LocationWindowInput in : windows) {
                LocalTime start = parseTime(in.startTime());
                LocalTime end = parseTime(in.endTime());
                if (!end.isAfter(start)) {
                    throw new BusinessException("O fim da janela deve ser após o início.");
                }
                LocationShareWindow window = new LocationShareWindow();
                window.setTransporter(transporter);
                window.setDayOfWeek(in.dayOfWeek());
                window.setStartTime(start);
                window.setEndTime(end);
                transporter.getLocationShareWindows().add(window);
            }
        }
        transporterRepository.save(transporter);
        return LocationSharingResponse.from(transporter);
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Horário inválido: " + value + " (use HH:mm).");
        }
    }

    private TransporterProfile requireTransporter(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }

    /** Soma o trecho percorrido desde o último ping ao km de hoje, com filtros. */
    private void accumulateDistance(TransporterProfile transporter, double lat, double lon) {
        Double prevLat = transporter.getCurrentLatitude();
        Double prevLon = transporter.getCurrentLongitude();
        Instant prevAt = transporter.getLocationUpdatedAt();
        if (prevLat == null || prevLon == null || prevAt == null) {
            return;
        }
        if (Duration.between(prevAt, Instant.now()).compareTo(MAX_GAP) > 0) {
            return;
        }
        double segment = GeoUtils.haversineKm(prevLat, prevLon, lat, lon);
        if (segment < MIN_SEGMENT_KM || segment > MAX_SEGMENT_KM) {
            return;
        }

        LocalDate today = LocalDate.now();
        DailyDistance daily = dailyDistanceRepository
                .findByTransporterIdAndDate(transporter.getId(), today)
                .orElseGet(() -> {
                    DailyDistance created = new DailyDistance();
                    created.setTransporter(transporter);
                    created.setDate(today);
                    created.setKm(0);
                    return created;
                });
        daily.setKm(daily.getKm() + segment);
        dailyDistanceRepository.save(daily);
    }
}
