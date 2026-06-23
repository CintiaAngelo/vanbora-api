package com.vanbora.api.modules.auth.service;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.LoginRequest;
import com.vanbora.api.modules.auth.dto.RegisterGuardianRequest;
import com.vanbora.api.modules.auth.dto.RegisterTransporterRequest;
import com.vanbora.api.modules.auth.dto.UserResponse;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.exception.BusinessException;
import java.math.BigDecimal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação dos casos de uso de autenticação. */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final GuardianProfileRepository guardianProfileRepository;
    private final TransporterProfileRepository transporterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(
            UserRepository userRepository,
            GuardianProfileRepository guardianProfileRepository,
            TransporterProfileRepository transporterProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.guardianProfileRepository = guardianProfileRepository;
        this.transporterProfileRepository = transporterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse registerGuardian(RegisterGuardianRequest request) {
        ensureEmailAvailable(request.email());

        User user = createUser(request.name(), request.email(), request.password(),
                request.phone(), UserRole.GUARDIAN);

        GuardianProfile profile = new GuardianProfile();
        profile.setUser(user);
        profile.setCpf(request.cpf());
        profile.setCep(request.cep());
        profile.setCity(request.city());
        profile.setNeighborhood(request.neighborhood());
        guardianProfileRepository.save(profile);

        return buildResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerTransporter(RegisterTransporterRequest request) {
        ensureEmailAvailable(request.email());

        User user = createUser(request.name(), request.email(), request.password(),
                request.phone(), UserRole.TRANSPORTER);

        TransporterProfile profile = new TransporterProfile();
        profile.setUser(user);
        profile.setDocument(request.document());
        profile.setCnh(request.cnh());
        profile.setPlate(request.plate());
        profile.setCapacity(request.capacity());
        profile.setAvailableSeats(request.capacity() == null ? 0 : request.capacity());
        profile.setBaseMonthlyFee(BigDecimal.ZERO);
        transporterProfileRepository.save(profile);

        return buildResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        return buildResponse(user);
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe uma conta com este e-mail.");
        }
    }

    private User createUser(String name, String email, String rawPassword, String phone, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(role);
        return userRepository.save(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationMillis(), UserResponse.from(user));
    }
}
