package com.vanbora.api.modules.auth;

import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
import com.vanbora.api.modules.auth.dto.ConsentStatusResponse;
import com.vanbora.api.modules.auth.dto.PushTokenRequest;
import com.vanbora.api.modules.auth.dto.UpdateOnboardingRequest;
import com.vanbora.api.modules.auth.service.AuthService;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.security.CurrentUserProvider;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.security.jwt.TokenRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conta do usuário autenticado. Fica fora de {@code /api/auth/**} (que é público)
 * para que estas operações exijam autenticação.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AuthService authService;
    private final PushNotificationService pushNotificationService;
    private final CurrentUserProvider currentUserProvider;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public AccountController(AuthService authService,
                            PushNotificationService pushNotificationService,
                            CurrentUserProvider currentUserProvider,
                            JwtService jwtService,
                            TokenRevocationService tokenRevocationService) {
        this.authService = authService;
        this.pushNotificationService = pushNotificationService;
        this.currentUserProvider = currentUserProvider;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserProvider.requireUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /** Situação do consentimento (LGPD) do usuário autenticado. */
    @GetMapping("/consents")
    public ConsentStatusResponse consentStatus() {
        return authService.getConsentStatus(currentUserProvider.requireUserId());
    }

    /** Revoga o consentimento vigente (LGPD, art. 8º §5º). */
    @PostMapping("/consents/revoke")
    public ConsentStatusResponse revokeConsent() {
        return authService.revokeConsent(currentUserProvider.requireUserId());
    }

    /** Registra o token de push (Expo) do aparelho do usuário. */
    @PostMapping("/push-token")
    public ResponseEntity<Void> registerPushToken(@Valid @RequestBody PushTokenRequest request) {
        pushNotificationService.registerToken(
                currentUserProvider.requireUserId(), request.token(), request.platform());
        return ResponseEntity.noContent().build();
    }

    /** Remove o token de push (ex.: logout). */
    @DeleteMapping("/push-token")
    public ResponseEntity<Void> removePushToken(@Valid @RequestBody PushTokenRequest request) {
        pushNotificationService.removeToken(currentUserProvider.requireUserId(), request.token());
        return ResponseEntity.noContent().build();
    }

    /** Registra que o usuário viu (ou dispensou) uma versão do guia de funcionalidades. */
    @PutMapping("/onboarding")
    public ResponseEntity<Void> updateOnboarding(@Valid @RequestBody UpdateOnboardingRequest request) {
        authService.updateOnboardingProgress(currentUserProvider.requireUserId(), request.seenVersion());
        return ResponseEntity.noContent().build();
    }

    /**
     * Encerra a sessão invalidando o token no servidor.
     *
     * Apagar o token no aparelho não basta: um JWT vale até expirar, então uma cópia
     * feita antes do logout continuaria aceita. Aqui o token entra na denylist e é
     * recusado a partir da próxima requisição — inclusive no WebSocket do chat.
     * Só este token é revogado; as sessões do usuário em outros aparelhos seguem.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            if (jwtService.isValid(token)) {
                tokenRevocationService.revoke(
                        jwtService.extractTokenId(token),
                        jwtService.extractExpiration(token),
                        currentUserProvider.requireUserId());
            }
        }
        return ResponseEntity.noContent().build();
    }
}

