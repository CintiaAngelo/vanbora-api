package com.vanbora.api.config;

import com.vanbora.api.modules.chat.service.ChatMembershipService;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.HelperRepository;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demonstração do perfil Monitor, aplicada também a bancos já populados (idempotente):
 * dá ao ajudante "Carlos Mendes" do Roberto uma conta de monitor e cria dois percursos —
 * "Ida — manhã" (com o Carlos) e "Volta — tarde" (sem monitor) — para demonstrar o
 * isolamento por percurso, o checklist e o chat em grupo.
 *
 * Login do monitor: monitor.carlos@vanbora.com / 123456.
 */
@Component
public class MonitorDemoSeeder {

    static final String MONITOR_EMAIL = "monitor.carlos@vanbora.com";
    private static final Logger log = LoggerFactory.getLogger(MonitorDemoSeeder.class);

    private final UserRepository userRepository;
    private final TransporterProfileRepository transporterRepository;
    private final HelperRepository helperRepository;
    private final TripRepository tripRepository;
    private final ChatMembershipService chatMembershipService;

    public MonitorDemoSeeder(UserRepository userRepository,
                             TransporterProfileRepository transporterRepository,
                             HelperRepository helperRepository,
                             TripRepository tripRepository,
                             ChatMembershipService chatMembershipService) {
        this.userRepository = userRepository;
        this.transporterRepository = transporterRepository;
        this.helperRepository = helperRepository;
        this.tripRepository = tripRepository;
        this.chatMembershipService = chatMembershipService;
    }

    @Transactional
    public void seed(String passwordHash) {
        if (userRepository.existsByEmail(MONITOR_EMAIL)) {
            return;
        }
        User robertoUser = userRepository.findByEmail("roberto@vanbora.com").orElse(null);
        if (robertoUser == null) {
            return;
        }
        TransporterProfile roberto = transporterRepository.findByUserId(robertoUser.getId()).orElse(null);
        if (roberto == null || !tripRepository.findActiveByTransporter(roberto.getId()).isEmpty()) {
            return; // já tem percursos próprios: não mexe na configuração do usuário
        }
        Helper carlos = helperRepository.findByTransporterId(roberto.getId()).stream()
                .filter(h -> h.getName().equals("Carlos Mendes") && h.getUser() == null)
                .findFirst()
                .orElse(null);
        if (carlos == null) {
            return;
        }

        User account = new User();
        account.setName(carlos.getName());
        account.setEmail(MONITOR_EMAIL);
        account.setPasswordHash(passwordHash);
        account.setPhone("(11) 97654-3210");
        account.setRole(UserRole.MONITOR);
        account.setActive(true);
        carlos.setUser(userRepository.save(account));
        helperRepository.save(carlos);

        Trip morning = new Trip();
        morning.setTransporter(roberto);
        morning.setName("Ida — manhã");
        morning.setAllStudents(true);
        morning.getMonitors().add(carlos);
        tripRepository.save(morning);

        Trip afternoon = new Trip();
        afternoon.setTransporter(roberto);
        afternoon.setName("Volta — tarde");
        afternoon.setAllStudents(true);
        tripRepository.save(afternoon);

        chatMembershipService.syncTransporter(roberto.getId());
        log.info("Demonstração do monitor criada: {} / 123456", MONITOR_EMAIL);
    }
}
