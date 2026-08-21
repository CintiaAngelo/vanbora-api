package com.vanbora.api.modules.auth;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.LoginRequest;
import com.vanbora.api.modules.auth.dto.RegisterGuardianRequest;
import com.vanbora.api.modules.auth.dto.RegisterTransporterRequest;
import com.vanbora.api.modules.auth.dto.UserResponse;
import com.vanbora.api.modules.auth.service.AuthService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de autenticação e cadastro. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register/guardian")
    public ResponseEntity<AuthResponse> registerGuardian(@Valid @RequestBody RegisterGuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerGuardian(request));
    }

    @PostMapping("/register/transporter")
    public ResponseEntity<AuthResponse> registerTransporter(@Valid @RequestBody RegisterTransporterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTransporter(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser(currentUserProvider.requireUserId()));
    }
}
