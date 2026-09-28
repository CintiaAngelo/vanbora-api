package com.vanbora.api.modules.monitor.service;

import com.vanbora.api.modules.chat.service.ChatMembershipService;
import com.vanbora.api.modules.monitor.dto.MonitorAccessRequest;
import com.vanbora.api.modules.notification.repository.PushTokenRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.trip.domain.Trip;
import com.vanbora.api.modules.trip.repository.TripRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.validation.DocumentValidations;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida da conta de acesso de um monitor — sempre comandado pelo transportador dono
 * do ajudante (a posse é conferida por quem chama, em {@code TransporterServiceImpl}).
 *
 * <p>Regra de histórico: a conta (User) NUNCA é apagada. Checklists, mensagens e a escala dos
 * percursos referenciam o usuário; ao remover o monitor a conta é desativada e anonimizada
 * (e-mail liberado para reuso, senha inutilizada, aparelhos de push removidos), mantendo só o
 * nome para que o histórico continue legível ("Realizada por: João — Monitor").
 */
@Service
public class MonitorAccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PushTokenRepository pushTokenRepository;
    private final TripRepository tripRepository;
    private final ChatMembershipService chatMembershipService;

    public MonitorAccountService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 PushTokenRepository pushTokenRepository,
                                 TripRepository tripRepository,
                                 ChatMembershipService chatMembershipService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pushTokenRepository = pushTokenRepository;
        this.tripRepository = tripRepository;
        this.chatMembershipService = chatMembershipService;
    }

    /** A função do ajudante é de monitor ("Monitor", "Monitora")? Só esses recebem acesso. */
    public static boolean isMonitorRole(String role) {
        if (role == null) {
            return false;
        }
        String plain = Normalizer.normalize(role, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return plain.startsWith("monitor");
    }

    /** Cria ou atualiza o acesso do monitor (e-mail, telefone e, opcionalmente, a senha). */
    @Transactional
    public void grantOrUpdateAccess(Helper helper, MonitorAccessRequest request) {
        if (!isMonitorRole(helper.getRole())) {
            throw new BusinessException("Apenas ajudantes com a função Monitor podem ter acesso ao app.");
        }
        String email = normalizeEmail(request.email());
        String phone = request.phone() == null ? "" : request.phone().trim();
        if (!DocumentValidations.isValidEmail(email)) {
            throw new BusinessException("E-mail inválido. Ex.: nome@exemplo.com.");
        }
        if (!DocumentValidations.isValidPhone(phone)) {
            throw new BusinessException("Telefone inválido. Informe DDD + número (11 dígitos).");
        }

        User user = helper.getUser();
        boolean creating = user == null;
        String password = request.password() == null ? "" : request.password();
        if (creating && password.isBlank()) {
            throw new BusinessException("Defina uma senha provisória para o monitor.");
        }
        if (!password.isBlank() && password.length() < 6) {
            throw new BusinessException("A senha deve ter ao menos 6 caracteres.");
        }
        userRepository.findByEmail(email)
                .filter(other -> creating || !other.getId().equals(user.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Já existe uma conta com este e-mail.");
                });

        User account = creating ? new User() : user;
        if (creating) {
            account.setRole(UserRole.MONITOR);
        }
        account.setName(helper.getName());
        account.setEmail(email);
        account.setPhone(phone);
        if (!password.isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(password));
        }
        account.setActive(helper.isActive());
        helper.setUser(userRepository.save(account));
        chatMembershipService.syncTransporter(helper.getTransporter().getId());
    }

    /**
     * Reflete na conta as mudanças feitas no ajudante: nome (histórico usa o novo nome daqui
     * em diante) e situação ativo/inativo — desativar revoga o acesso na hora.
     */
    @Transactional
    public void syncWithHelper(Helper helper) {
        User user = helper.getUser();
        if (user == null) {
            return;
        }
        if (!isMonitorRole(helper.getRole())) {
            // Deixou de ser monitor: perde o acesso (a conta fica para o histórico).
            revokeAccess(helper);
            return;
        }
        user.setName(helper.getName());
        user.setActive(helper.isActive());
        userRepository.save(user);
        if (!helper.isActive()) {
            pushTokenRepository.deleteByUserId(user.getId());
        }
        chatMembershipService.syncTransporter(helper.getTransporter().getId());
    }

    /**
     * Retira o acesso ao app mantendo o ajudante no perfil. A conta antiga é desativada e
     * anonimizada; um novo acesso cria outra conta.
     */
    @Transactional
    public void revokeAccess(Helper helper) {
        User user = helper.getUser();
        if (user == null) {
            return;
        }
        deactivateAndAnonymize(user);
        helper.setUser(null);
        chatMembershipService.syncTransporter(helper.getTransporter().getId());
    }

    /**
     * Prepara a exclusão do ajudante: revoga o acesso e o tira de todos os percursos. O
     * histórico (checklists, execuções, mensagens) aponta para a conta, que permanece.
     */
    @Transactional
    public void beforeHelperRemoval(Helper helper) {
        for (Trip trip : tripRepository.findAllByMonitor(helper.getId())) {
            trip.getMonitors().removeIf(h -> h.getId().equals(helper.getId()));
            tripRepository.save(trip);
        }
        if (helper.getUser() != null) {
            deactivateAndAnonymize(helper.getUser());
            helper.setUser(null);
        }
        chatMembershipService.syncTransporter(helper.getTransporter().getId());
    }

    private void deactivateAndAnonymize(User user) {
        user.setActive(false);
        // Libera o e-mail para reuso e inutiliza a senha; o nome fica para o histórico.
        user.setEmail("removido+%d.%s@monitor.vanbora.invalid".formatted(user.getId(), randomToken(6)));
        user.setPasswordHash(passwordEncoder.encode(randomToken(32)));
        user.setPhone("");
        user.setAuthProvider(null);
        user.setAuthSubject(null);
        userRepository.save(user);
        pushTokenRepository.deleteByUserId(user.getId());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String randomToken(int bytes) {
        byte[] raw = new byte[bytes];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
