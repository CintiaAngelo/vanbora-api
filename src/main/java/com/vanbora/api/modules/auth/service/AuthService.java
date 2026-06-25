package com.vanbora.api.modules.auth.service;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
import com.vanbora.api.modules.auth.dto.LoginRequest;
import com.vanbora.api.modules.auth.dto.RegisterGuardianRequest;
import com.vanbora.api.modules.auth.dto.RegisterTransporterRequest;

/** Casos de uso de autenticação e cadastro (abstração para inversão de dependência). */
public interface AuthService {

    AuthResponse registerGuardian(RegisterGuardianRequest request);

    AuthResponse registerTransporter(RegisterTransporterRequest request);

    AuthResponse login(LoginRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
