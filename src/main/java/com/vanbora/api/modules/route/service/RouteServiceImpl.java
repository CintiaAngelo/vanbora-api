package com.vanbora.api.modules.route.service;

import com.vanbora.api.modules.enrollment.repository.AbsenceRepository;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.dto.RouteResponse;
import com.vanbora.api.modules.route.dto.RouteStopResponse;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import com.vanbora.api.shared.eta.EtaEstimator;
import com.vanbora.api.shared.geo.RoutingService;
import com.vanbora.api.shared.util.GeoUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação da rota do dia, incluindo otimização vizinho-mais-próximo. */
@Service
public class RouteServiceImpl implements RouteService {

    private final RouteStopRepository routeStopRepository;
    private final TransporterProfileRepository transporterRepository;
    private final AbsenceRepository absenceRepository;
    private final EtaEstimator etaEstimator;
    private final RoutingService routingService;

    public RouteServiceImpl(RouteStopRepository routeStopRepository,
                            TransporterProfileRepository transporterRepository,
                            AbsenceRepository absenceRepository,
                            EtaEstimator etaEstimator,
                            RoutingService routingService) {
        this.routeStopRepository = routeStopRepository;
        this.transporterRepository = transporterRepository;
        this.absenceRepository = absenceRepository;
        this.etaEstimator = etaEstimator;
        this.routingService = routingService;
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse listForUser(Long userId) {
        TransporterProfile transporter = requireTransporter(userId);
        List<RouteStop> stops = routeStopRepository
                .findByTransporterIdOrderByPositionAsc(transporter.getId());
        Set<Long> absentDependentIds = absenceRepository.findAbsentDependentIds(transporter.getId(), LocalDate.now());

        // Mantém a ordem base (persistida) das paradas ativas; quem faltou hoje só é
        // excluído do trajeto/numeração, sem alterar nada permanentemente.
        List<RouteStop> activePickups = new ArrayList<>();
        List<RouteStop> schools = new ArrayList<>();
        List<RouteStop> notGoingToday = new ArrayList<>();
        for (RouteStop stop : stops) {
            if (stop.getStatus() == RouteStopStatus.SCHOOL) {
                schools.add(stop);
            } else if (isAbsentToday(stop, absentDependentIds)) {
                notGoingToday.add(stop);
            } else {
                activePickups.add(stop);
            }
        }
        Double lat = transporter.getCurrentLatitude();
        Double lon = transporter.getCurrentLongitude();
        List<RouteStopResponse> stopsView = dayView(activePickups, schools, notGoingToday, lat, lon);
        return new RouteResponse(stopsView, routeGeometry(activePickups, schools, lat, lon));
    }

    @Override
    @Transactional
    public RouteResponse optimizeForUser(Long userId) {
        TransporterProfile transporter = requireTransporter(userId);
        List<RouteStop> stops = routeStopRepository
                .findByTransporterIdOrderByPositionAsc(transporter.getId());
        Set<Long> absentDependentIds = absenceRepository.findAbsentDependentIds(transporter.getId(), LocalDate.now());

        // Separa paradas de embarque (com coordenadas) das de escola/destino, e deixa de
        // fora quem avisou falta hoje — não gera desvio na rota nem tem a posição base
        // (persistida) alterada, para voltar ao normal sozinho quando a falta for cancelada.
        List<RouteStop> pickups = new ArrayList<>();
        List<RouteStop> schools = new ArrayList<>();
        List<RouteStop> notGoingToday = new ArrayList<>();
        for (RouteStop stop : stops) {
            if (stop.getStatus() == RouteStopStatus.SCHOOL) {
                schools.add(stop);
            } else if (isAbsentToday(stop, absentDependentIds)) {
                notGoingToday.add(stop);
            } else {
                pickups.add(stop);
            }
        }

        List<RouteStop> orderedPickups = nearestNeighbor(pickups,
                transporter.getCurrentLatitude(), transporter.getCurrentLongitude());

        List<RouteStop> ordered = new ArrayList<>(orderedPickups);
        ordered.addAll(schools); // escola(s) sempre como destino final
        int position = 1;
        for (RouteStop stop : ordered) {
            stop.setPosition(position++);
        }
        routeStopRepository.saveAll(ordered);

        Double lat = transporter.getCurrentLatitude();
        Double lon = transporter.getCurrentLongitude();
        List<RouteStopResponse> stopsView = dayView(orderedPickups, schools, notGoingToday, lat, lon);
        return new RouteResponse(stopsView, routeGeometry(orderedPickups, schools, lat, lon));
    }

    /**
     * Trajeto real (seguindo ruas) da posição atual da van até a escola, passando
     * pelas paradas ativas na ordem dada. Null se a van ainda não compartilhou
     * posição, ou se o roteamento não estiver disponível (ver {@link RoutingService}).
     */
    private List<double[]> routeGeometry(List<RouteStop> activePickups, List<RouteStop> schools,
                                         Double startLat, Double startLon) {
        if (startLat == null || startLon == null) {
            return null;
        }
        List<double[]> waypoints = new ArrayList<>();
        waypoints.add(new double[] {startLat, startLon});
        for (RouteStop stop : activePickups) {
            if (stop.getLatitude() != null && stop.getLongitude() != null) {
                waypoints.add(new double[] {stop.getLatitude(), stop.getLongitude()});
            }
        }
        for (RouteStop stop : schools) {
            if (stop.getLatitude() != null && stop.getLongitude() != null) {
                waypoints.add(new double[] {stop.getLatitude(), stop.getLongitude()});
            }
        }
        return routingService.route(waypoints).orElse(null);
    }

    private boolean isAbsentToday(RouteStop stop, Set<Long> absentDependentIds) {
        return stop.getDependent() != null && absentDependentIds.contains(stop.getDependent().getId());
    }

    /**
     * Monta a rota do dia: paradas ativas com trajeto/ETA calculados em sequência (escola
     * por último), com quem avisou falta hoje intercalado só para visibilidade — sem
     * posição no trajeto nem estimativas, já que o transportador não vai parar lá.
     */
    private List<RouteStopResponse> dayView(List<RouteStop> activePickups, List<RouteStop> schools,
                                            List<RouteStop> notGoingToday, Double startLat, Double startLon) {
        List<RouteStop> forEta = new ArrayList<>(activePickups);
        forEta.addAll(schools);
        List<RouteStopResponse> annotated = annotate(forEta, startLat, startLon);

        List<RouteStopResponse> result = new ArrayList<>(annotated.subList(0, activePickups.size()));
        notGoingToday.forEach(stop -> result.add(RouteStopResponse.notGoingToday(stop)));
        result.addAll(annotated.subList(activePickups.size(), annotated.size()));
        return result;
    }

    /**
     * Anota cada parada (na ordem dada) com a distância do ponto anterior, a
     * distância acumulada, os minutos desde a partida e o horário previsto de
     * chegada, partindo da posição atual do transportador.
     */
    private List<RouteStopResponse> annotate(List<RouteStop> ordered, Double startLat, Double startLon) {
        List<RouteStopResponse> result = new ArrayList<>();
        Double prevLat = startLat;
        Double prevLon = startLon;
        double cumulative = 0.0;

        for (RouteStop stop : ordered) {
            Double leg = null;
            Double cumOut = null;
            Integer etaMin = null;
            String etaClock = null;

            if (stop.getLatitude() != null && stop.getLongitude() != null) {
                if (prevLat != null && prevLon != null) {
                    double straight = GeoUtils.haversineKm(
                            prevLat, prevLon, stop.getLatitude(), stop.getLongitude());
                    leg = round1(etaEstimator.roadKm(straight));
                    cumulative += leg;
                } else {
                    leg = 0.0;
                }
                cumOut = round1(cumulative);
                etaMin = etaEstimator.minutes(cumulative);
                etaClock = etaEstimator.clockFromNow(etaMin);
                prevLat = stop.getLatitude();
                prevLon = stop.getLongitude();
            }
            result.add(RouteStopResponse.from(stop, leg, cumOut, etaMin, etaClock));
        }
        return result;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
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
