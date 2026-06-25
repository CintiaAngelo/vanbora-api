package com.vanbora.api.modules.route.service;

import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.util.GeoUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação da rota do dia, incluindo otimização vizinho-mais-próximo. */
@Service
public class RouteServiceImpl implements RouteService {

    private final RouteStopRepository routeStopRepository;
    private final TransporterProfileRepository transporterRepository;

    public RouteServiceImpl(RouteStopRepository routeStopRepository,
                            TransporterProfileRepository transporterRepository) {
        this.routeStopRepository = routeStopRepository;
        this.transporterRepository = transporterRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteStopResponse> listForUser(Long userId) {
        Long transporterId = requireTransporter(userId).getId();
        return routeStopRepository.findByTransporterIdOrderByPositionAsc(transporterId).stream()
                .map(RouteStopResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<RouteStopResponse> optimizeForUser(Long userId) {
        TransporterProfile transporter = requireTransporter(userId);
        List<RouteStop> stops = routeStopRepository
                .findByTransporterIdOrderByPositionAsc(transporter.getId());

        // Separa paradas de embarque (com coordenadas) das de escola/destino.
        List<RouteStop> pickups = new ArrayList<>();
        List<RouteStop> schools = new ArrayList<>();
        for (RouteStop stop : stops) {
            if (stop.getStatus() == RouteStopStatus.SCHOOL) {
                schools.add(stop);
            } else {
                pickups.add(stop);
            }
        }

        List<RouteStop> ordered = nearestNeighbor(pickups,
                transporter.getCurrentLatitude(), transporter.getCurrentLongitude());
        ordered.addAll(schools); // escola(s) sempre como destino final

        int position = 1;
        for (RouteStop stop : ordered) {
            stop.setPosition(position++);
        }
        routeStopRepository.saveAll(ordered);

        return ordered.stream().map(RouteStopResponse::from).toList();
    }

    /**
     * Ordena paradas pela mais próxima a cada passo. Paradas sem coordenada
     * mantêm a ordem relativa original e vão para o fim da sequência.
     */
    private List<RouteStop> nearestNeighbor(List<RouteStop> pickups, Double startLat, Double startLon) {
        List<RouteStop> withCoords = new ArrayList<>();
        List<RouteStop> withoutCoords = new ArrayList<>();
        for (RouteStop stop : pickups) {
            if (stop.getLatitude() != null && stop.getLongitude() != null) {
                withCoords.add(stop);
            } else {
                withoutCoords.add(stop);
            }
        }

        List<RouteStop> result = new ArrayList<>();
        List<RouteStop> remaining = new ArrayList<>(withCoords);

        // Ponto de partida: posição atual do transportador, ou a 1ª parada com coordenada.
        double currentLat;
        double currentLon;
        if (startLat != null && startLon != null) {
            currentLat = startLat;
            currentLon = startLon;
        } else if (!remaining.isEmpty()) {
            RouteStop first = remaining.remove(0);
            result.add(first);
            currentLat = first.getLatitude();
            currentLon = first.getLongitude();
        } else {
            result.addAll(withoutCoords);
            return result;
        }

        while (!remaining.isEmpty()) {
            RouteStop nearest = null;
            double best = Double.MAX_VALUE;
            for (RouteStop candidate : remaining) {
                double d = GeoUtils.haversineKm(currentLat, currentLon,
                        candidate.getLatitude(), candidate.getLongitude());
                if (d < best) {
                    best = d;
                    nearest = candidate;
                }
            }
            result.add(nearest);
            remaining.remove(nearest);
            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
        }

        result.addAll(withoutCoords);
        return result;
    }

    private TransporterProfile requireTransporter(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }
}
