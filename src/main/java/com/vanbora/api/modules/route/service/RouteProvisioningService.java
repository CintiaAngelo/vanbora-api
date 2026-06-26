package com.vanbora.api.modules.route.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.geo.GeocodingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Provisiona as paradas de rota quando um aluno é matriculado (contrato assinado):
 * cria a parada de embarque do aluno (coordenadas da casa) e garante a parada da
 * escola correspondente. Idempotente — não duplica paradas já existentes.
 */
@Service
public class RouteProvisioningService {

    private final RouteStopRepository routeStopRepository;
    private final GeocodingService geocodingService;

    public RouteProvisioningService(RouteStopRepository routeStopRepository,
                                    GeocodingService geocodingService) {
        this.routeStopRepository = routeStopRepository;
        this.geocodingService = geocodingService;
    }

    @Transactional
    public void provisionForEnrollment(Enrollment enrollment) {
        TransporterProfile transporter = enrollment.getTransporter();
        Dependent dependent = enrollment.getDependent();
        GuardianProfile guardian = dependent.getGuardian();

        List<RouteStop> existing =
                routeStopRepository.findByTransporterIdOrderByPositionAsc(transporter.getId());
        int nextPosition = existing.stream().mapToInt(RouteStop::getPosition).max().orElse(0) + 1;

        // Parada de embarque do aluno (uma por dependente).
        boolean hasPickup = existing.stream()
                .anyMatch(s -> s.getDependent() != null
                        && s.getDependent().getId().equals(dependent.getId()));
        if (!hasPickup) {
            RouteStop pickup = new RouteStop();
            pickup.setTransporter(transporter);
            pickup.setDependent(dependent);
            pickup.setLabel(dependent.getName());
            pickup.setAddress(homeAddress(guardian));
            pickup.setStatus(RouteStopStatus.GOING);
            pickup.setPosition(nextPosition++);
            pickup.setLatitude(guardian.getLatitude());
            pickup.setLongitude(guardian.getLongitude());
            routeStopRepository.save(pickup);
        }

        // Parada da escola (uma por escola atendida pelo transportador).
        String school = dependent.getSchool();
        boolean hasSchool = existing.stream()
                .anyMatch(s -> s.getStatus() == RouteStopStatus.SCHOOL
                        && s.getLabel() != null && s.getLabel().equalsIgnoreCase(school));
        if (!hasSchool && StringUtils.hasText(school)) {
            RouteStop schoolStop = new RouteStop();
            schoolStop.setTransporter(transporter);
            schoolStop.setLabel(school);
            schoolStop.setAddress(school);
            schoolStop.setStatus(RouteStopStatus.SCHOOL);
            schoolStop.setPosition(nextPosition);

            // Preferência: coordenada precisa do catálogo (escola escolhida no cadastro do dependente).
            var catalog = dependent.getSchoolRef();
            if (catalog != null && catalog.getLatitude() != null && catalog.getLongitude() != null) {
                schoolStop.setLatitude(catalog.getLatitude());
                schoolStop.setLongitude(catalog.getLongitude());
            } else {
                // Fallback (dados antigos sem escola do catálogo): geocodifica pelo nome.
                geocodingService.geocode(school + ", " + safe(guardian.getCity()) + ", Brasil")
                        .ifPresent(coord -> {
                            schoolStop.setLatitude(coord[0]);
                            schoolStop.setLongitude(coord[1]);
                        });
            }
            routeStopRepository.save(schoolStop);
        }
    }

    /**
     * Remove o aluno da rota ao cancelar a matrícula (sua parada de embarque some
     * do mapa do transportador). A parada da escola permanece — é compartilhada.
     */
    @Transactional
    public void deprovisionForDependent(Long transporterId, Long dependentId) {
        routeStopRepository.deleteByTransporterIdAndDependentId(transporterId, dependentId);
    }

    private String homeAddress(GuardianProfile guardian) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, guardian.getStreet());
        if (StringUtils.hasText(guardian.getNumber())) {
            sb.append(sb.length() > 0 ? ", " : "").append(guardian.getNumber());
        }
        appendPart(sb, guardian.getNeighborhood());
        return sb.length() > 0 ? sb.toString() : safe(guardian.getCity());
    }

    private void appendPart(StringBuilder sb, String part) {
        if (StringUtils.hasText(part)) {
            sb.append(sb.length() > 0 ? ", " : "").append(part.trim());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
