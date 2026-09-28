package com.vanbora.api.support;

import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Monta cenários de teste com as entidades reais (persistidas no H2), no formato
 * Transportador → Percurso → Monitor → Alunos. E-mails únicos por chamada permitem
 * reusar o mesmo banco entre testes que confirmam transações.
 */
@Component
public class TestFixtures {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final UserRepository userRepository;
    private final TransporterProfileRepository transporterRepository;
    private final GuardianProfileRepository guardianRepository;
    private final DependentRepository dependentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final HelperRepository helperRepository;
    private final TripRepository tripRepository;

    public TestFixtures(UserRepository userRepository,
                        TransporterProfileRepository transporterRepository,
                        GuardianProfileRepository guardianRepository,
                        DependentRepository dependentRepository,
                        EnrollmentRepository enrollmentRepository,
                        HelperRepository helperRepository,
                        TripRepository tripRepository) {
        this.userRepository = userRepository;
        this.transporterRepository = transporterRepository;
        this.guardianRepository = guardianRepository;
        this.dependentRepository = dependentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.helperRepository = helperRepository;
        this.tripRepository = tripRepository;
    }

    public User user(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail("u" + SEQ.incrementAndGet() + "." + System.nanoTime() + "@teste.com");
        user.setPasswordHash("{noop}x");
        user.setPhone("11999990000");
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    public TransporterProfile transporter(String name) {
        TransporterProfile profile = new TransporterProfile();
        profile.setUser(user(name, UserRole.TRANSPORTER));
        profile.setPlate("ABC1D23");
        return transporterRepository.save(profile);
    }

    public GuardianProfile guardian(String name) {
        GuardianProfile profile = new GuardianProfile();
        profile.setUser(user(name, UserRole.GUARDIAN));
        profile.setNeighborhood("Vila Mariana");
        return guardianRepository.save(profile);
    }

    public Dependent dependent(GuardianProfile guardian, String name) {
        Dependent dependent = new Dependent();
        dependent.setGuardian(guardian);
        dependent.setName(name);
        dependent.setSchool("Colégio Objetivo");
        return dependentRepository.save(dependent);
    }

    /** Contratação concluída (matrícula ativa). */
    public Enrollment enroll(TransporterProfile transporter, Dependent dependent) {
        Enrollment enrollment = new Enrollment();
        enrollment.setTransporter(transporter);
        enrollment.setDependent(dependent);
        enrollment.setMonthlyFee(new BigDecimal("400.00"));
        enrollment.setFinanceStatus(FinanceStatus.ATRASADO);
        enrollment.setActive(true);
        return enrollmentRepository.save(enrollment);
    }

    /** Ajudante com função Monitor e conta de acesso ativa. */
    public Helper monitor(TransporterProfile transporter, String name) {
        Helper helper = new Helper();
        helper.setTransporter(transporter);
        helper.setName(name);
        helper.setRole("Monitor");
        helper.setActive(true);
        User account = user(name, UserRole.MONITOR);
        helper.setUser(account);
        return helperRepository.save(helper);
    }

    /** Percurso com monitores e, se informados, só estes alunos (senão: todos). */
    public Trip trip(TransporterProfile transporter, String name, List<Helper> monitors, List<Dependent> students) {
        Trip trip = new Trip();
        trip.setTransporter(transporter);
        trip.setName(name);
        trip.setActive(true);
        trip.getMonitors().addAll(monitors);
        if (students == null) {
            trip.setAllStudents(true);
        } else {
            trip.setAllStudents(false);
            trip.getStudents().addAll(students);
        }
        return tripRepository.save(trip);
    }
}
