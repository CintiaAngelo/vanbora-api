package com.vanbora.api.modules.auth;

import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
import com.vanbora.api.modules.auth.service.AuthService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    private final CurrentUserProvider currentUserProvider;

    public AccountController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserProvider.requireUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
