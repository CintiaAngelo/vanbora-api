package com.vanbora.api.modules.auth.social;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.SocialAuthResponse;
import com.vanbora.api.modules.auth.dto.SocialAuthResponse.SocialProfile;
import com.vanbora.api.modules.auth.dto.UserResponse;
import com.vanbora.api.modules.user.domain.OnboardingProgress;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.OnboardingProgressRepository;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.shared.exception.UnauthorizedException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login com Google/Facebook via Auth0.
 *
 * O vínculo entre a conta social e a conta do VanBora é o e-mail — o mesmo
 * identificador do cadastro comum. Por isso só e-mail confirmado pelo provedor é
 * aceito: um provedor que não verifica e-mail permitiria criar uma conta social
 * com o e-mail de outra pessoa e assumir a conta dela aqui dentro.
 *
 * Vale para os dois perfis: responsável e transportador entram pelo mesmo
 * endpoint, e o papel vem da conta que já existe (ou é escolhido no cadastro).
 */
@Service
public class SocialAuthService {

    private final Auth0TokenVerifier tokenVerifier;
    private final SignupTicketService signupTicketService;
    private final UserRepository userRepository;
    private final OnboardingProgressRepository onboardingProgressRepository;
    private final JwtService jwtService;

    public SocialAuthService(Auth0TokenVerifier tokenVerifier,
                             SignupTicketService signupTicketService,
                             UserRepository userRepository,
                             OnboardingProgressRepository onboardingProgressRepository,
                             JwtService jwtService) {
        this.tokenVerifier = tokenVerifier;
        this.signupTicketService = signupTicketService;
        this.userRepository = userRepository;
        this.onboardingProgressRepository = onboardingProgressRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public SocialAuthResponse authenticate(String idToken) {
        SocialIdentity identity = tokenVerifier.verify(idToken);

        if (!identity.emailVerified()) {
            throw new UnauthorizedException(
                    "O provedor não confirmou este e-mail. Confirme-o na sua conta Google/Facebook "
                            + "ou cadastre-se com e-mail e senha.");
        }

        SocialProfile profile =
                new SocialProfile(identity.email(), identity.name(), identity.provider());

        Optional<User> existing = userRepository.findByEmail(identity.email());
        if (existing.isEmpty()) {
            return SocialAuthResponse.needsSignup(profile, signupTicketService.issue(identity));
        }

        User user = existing.get();
        linkProvider(user, identity);
        return SocialAuthResponse.loggedIn(buildSession(user), profile);
    }

    /**
     * Registra o provedor na conta que já existia (primeiro login social de quem
     * se cadastrou por e-mail/senha). Uma conta já vinculada a outro provedor
     * mantém o vínculo original: o login continua valendo, o campo é apenas
     * informativo de qual foi o primeiro.
     */
    private void linkProvider(User user, SocialIdentity identity) {
        if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
            user.setAuthProvider(identity.provider());
            user.setAuthSubject(identity.subject());
            userRepository.save(user);
        }
    }

    private AuthResponse buildSession(User user) {
        int onboardingVersion = onboardingProgressRepository.findByUserId(user.getId())
                .map(OnboardingProgress::getLastSeenVersion)
                .orElse(0);
        return AuthResponse.of(
                jwtService.generateToken(user),
                jwtService.getExpirationMillis(),
                UserResponse.from(user, onboardingVersion));
    }
}
