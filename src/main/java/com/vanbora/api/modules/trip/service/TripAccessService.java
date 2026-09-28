package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto único das regras de acesso a percursos, alunos e checklists — sempre derivadas
 * dos vínculos reais: Transportador → Percurso → Monitor → Alunos.
 *
 * <p>Nada aqui confia em ids vindos do cliente: o profissional é resolvido a partir do
 * usuário autenticado e todo recurso pedido por id é conferido contra ele. Recursos de
 * outro transportador ou de percursos em que o monitor não está escalado respondem 404
 * (não revelam que existem), evitando IDOR/BOLA.
 */
@Service
@Transactional(readOnly = true)
public class TripAccessService {

    public static final String ROLE_DRIVER = "Motorista";
    public static final String ROLE_MONITOR = "Monitor";

    private final TransporterProfileRepository transporterRepository;
    private final HelperRepository helperRepository;
    private final TripRepository tripRepository;
    private final EnrollmentRepository enrollmentRepository;

    public TripAccessService(TransporterProfileRepository transporterRepository,
                             HelperRepository helperRepository,
                             TripRepository tripRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.transporterRepository = transporterRepository;
        this.helperRepository = helperRepository;
        this.tripRepository = tripRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Profissional da operação: o transportador (que dirige a van — "Motorista") ou um
     * monitor com acesso ativo. {@code helper} é nulo para o transportador.
     */
    public record Professional(User user, TransporterProfile transporter, Helper helper, String roleLabel) {

        public boolean isTransporter() {
            return helper == null;
        }
    }

    /** Resolve o profissional autenticado ou recusa (responsável, escola, monitor desativado…). */
    public Professional requireProfessional(User user) {
        if (user.getRole() == UserRole.TRANSPORTER) {
            TransporterProfile transporter = transporterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", user.getId()));
            return new Professional(user, transporter, null, ROLE_DRIVER);
        }
        if (user.getRole() == UserRole.MONITOR) {
            Helper helper = requireActiveMonitor(user);
            return new Professional(user, helper.getTransporter(), helper, ROLE_MONITOR);
        }
        throw new AccessDeniedException("Perfil sem acesso à operação de transporte.");
    }

    /** Ajudante do monitor autenticado, exigindo que ele e a conta estejam ativos. */
    public Helper requireActiveMonitor(User user) {
        Helper helper = helperRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Monitor sem vínculo com um transportador."));
        if (!helper.hasActiveAccess()) {
            throw new AccessDeniedException("O acesso deste monitor foi desativado pelo transportador.");
        }
        return helper;
    }

    /** Percursos visíveis ao profissional: todos (transportador) ou só os escalados (monitor). */
    public List<Trip> tripsOf(Professional professional) {
        if (professional.isTransporter()) {
            return tripRepository.findActiveByTransporter(professional.transporter().getId());
        }
        return tripRepository.findActiveByMonitor(professional.helper().getId());
    }

    /** O profissional pode acessar este percurso? */
    public boolean canAccess(Professional professional, Trip trip) {
        if (!trip.getTransporter().getId().equals(professional.transporter().getId())) {
            return false; // isolamento entre transportadores
        }
        if (professional.isTransporter()) {
            return true;
        }
        return trip.isActive() && trip.hasMonitor(professional.helper().getId());
    }

    /** Percurso por id, conferido contra o profissional (404 se não tiver acesso). */
    public Trip requireTrip(Professional professional, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ResourceNotFoundException.of("Percurso", tripId));
        if (!canAccess(professional, trip)) {
            throw ResourceNotFoundException.of("Percurso", tripId);
        }
        return trip;
    }

    /**
     * Matrículas ATIVAS dos alunos do percurso (uma por aluno). Considera "todos os alunos"
     * ou a lista explícita; alunos com contrato encerrado nunca aparecem.
     */
    public List<Enrollment> activeEnrollmentsOf(Trip trip) {
        Map<Long, Enrollment> byDependent = new LinkedHashMap<>();
        for (Enrollment e : enrollmentRepository.findByTransporterIdAndActiveTrue(trip.getTransporter().getId())) {
            if (trip.includesDependent(e.getDependent().getId())) {
                byDependent.putIfAbsent(e.getDependent().getId(), e);
            }
        }
        return List.copyOf(byDependent.values());
    }
}
