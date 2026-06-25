package com.vanbora.api.modules.auth.service;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
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
import com.vanbora.api.modules.guardian.service.GuardianAddressService;
import com.vanbora.api.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
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
    private final GuardianAddressService guardianAddressService;

    public AuthServiceImpl(
            UserRepository userRepository,
            GuardianProfileRepository guardianProfileRepository,
            TransporterProfileRepository transporterProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            GuardianAddressService guardianAddressService) {
        this.userRepository = userRepository;
        this.guardianProfileRepository = guardianProfileRepository;
        this.transporterProfileRepository = transporterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.guardianAddressService = guardianAddressService;
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
        // Aplica embarque + entrega e geocodifica (best-effort: falha não impede o cadastro).
        guardianAddressService.apply(
                profile, request.pickup(), request.deliverySameAsPickup(), request.delivery());
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
        profile.setBaseMonthlyFee(
                request.baseMonthlyFee() != null ? request.baseMonthlyFee() : BigDecimal.ZERO);
        addCleaned(profile.getSchools(), request.schools());
        addCleaned(profile.getNeighborhoods(), request.neighborhoods());
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

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha deve ser diferente da atual.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Adiciona valores não-vazios (trim) ao conjunto, ignorando lista nula. */
    private void addCleaned(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
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
