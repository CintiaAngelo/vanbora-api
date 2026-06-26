package com.vanbora.api.modules.auth;

import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
import com.vanbora.api.modules.auth.dto.ConsentStatusResponse;
import com.vanbora.api.modules.auth.dto.PushTokenRequest;
import com.vanbora.api.modules.auth.service.AuthService;
import com.vanbora.api.modules.notification.PushNotificationService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    public AccountController(AuthService authService,
                            PushNotificationService pushNotificationService,
                            CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.pushNotificationService = pushNotificationService;
        this.currentUserProvider = currentUserProvider;
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
        pushNotificationService.removeToken(request.token());
        return ResponseEntity.noContent().build();
    }
}

