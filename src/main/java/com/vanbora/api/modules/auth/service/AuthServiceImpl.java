package com.vanbora.api.modules.auth.service;

import com.vanbora.api.modules.auth.dto.AuthResponse;
import com.vanbora.api.modules.auth.dto.ChangePasswordRequest;
import com.vanbora.api.modules.auth.dto.ConsentStatusResponse;
import com.vanbora.api.modules.auth.dto.LoginRequest;
import com.vanbora.api.modules.auth.dto.RegisterGuardianRequest;
import com.vanbora.api.modules.auth.dto.RegisterTransporterRequest;
import com.vanbora.api.modules.auth.dto.UserResponse;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.domain.OnboardingProgress;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.domain.UserConsent;
import com.vanbora.api.modules.user.repository.OnboardingProgressRepository;
import com.vanbora.api.modules.user.repository.UserConsentRepository;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.security.jwt.JwtService;
import com.vanbora.api.shared.enums.ConsentType;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.modules.guardian.service.GuardianAddressService;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.legal.LegalDocuments;
import com.vanbora.api.shared.validation.DocumentValidations;
import java.math.BigDecimal;
import java.time.Instant;
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
    private final UserConsentRepository userConsentRepository;
    private final OnboardingProgressRepository onboardingProgressRepository;
    private final GuardianProfileRepository guardianProfileRepository;
    private final TransporterProfileRepository transporterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GuardianAddressService guardianAddressService;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserConsentRepository userConsentRepository,
            OnboardingProgressRepository onboardingProgressRepository,
            GuardianProfileRepository guardianProfileRepository,
            TransporterProfileRepository transporterProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            GuardianAddressService guardianAddressService) {
        this.userRepository = userRepository;
        this.userConsentRepository = userConsentRepository;
        this.onboardingProgressRepository = onboardingProgressRepository;
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
        requireConsent(request.acceptedTerms());
        validateContact(request.phone(), request.email());
        if (!DocumentValidations.isValidCpf(request.cpf())) {
            throw new BusinessException("CPF inválido. Confira os números digitados.");
        }
        ensureEmailAvailable(request.email());

        User user = createUser(request.name(), request.email(), request.password(),
                request.phone(), UserRole.GUARDIAN);
        recordConsent(user);

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
        requireConsent(request.acceptedTerms());
        validateContact(request.phone(), request.email());
        if (!DocumentValidations.isValidCpf(request.document())) {
            throw new BusinessException("CPF inválido. Confira os números digitados.");
        }
        if (!DocumentValidations.isValidCnh(request.cnh())) {
            throw new BusinessException("CNH inválida. Informe os 11 dígitos do número de registro.");
        }
        if (!DocumentValidations.isValidPlate(request.plate())) {
            throw new BusinessException("Placa inválida. Use até 7 caracteres (letras e números).");
        }
        ensureEmailAvailable(request.email());

        User user = createUser(request.name(), request.email(), request.password(),
                request.phone(), UserRole.TRANSPORTER);

        TransporterProfile profile = new TransporterProfile();
        profile.setUser(user);
        profile.setDocument(request.document());
        profile.setCnh(request.cnh());
        profile.setPlate(request.plate());
        profile.setBaseMonthlyFee(
                request.baseMonthlyFee() != null ? request.baseMonthlyFee() : BigDecimal.ZERO);
        addCleaned(profile.getSchools(), request.schools());
        addCleaned(profile.getNeighborhoods(), request.neighborhoods());
        transporterProfileRepository.save(profile);
        recordConsent(user);

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
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        return UserResponse.from(user, lastSeenOnboardingVersion(userId));
    }

    @Override
    @Transactional
    public void updateOnboardingProgress(Long userId, int seenVersion) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        OnboardingProgress progress = onboardingProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    OnboardingProgress created = new OnboardingProgress();
                    created.setUser(user);
                    created.setLastSeenVersion(0);
                    return created;
                });
        // Nunca regride: o guia é cumulativo, uma marcação antiga não deve "desmarcar" uma nova.
        progress.setLastSeenVersion(Math.max(progress.getLastSeenVersion(), seenVersion));
        onboardingProgressRepository.save(progress);
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

    @Override
    @Transactional(readOnly = true)
    public ConsentStatusResponse getConsentStatus(Long userId) {
        return userConsentRepository
                .findFirstByUserIdAndTypeOrderByIdDesc(userId, ConsentType.PRIVACY_AND_TERMS)
                .map(c -> new ConsentStatusResponse(
                        c.getRevokedAt() == null, c.getDocumentVersion(), c.getAcceptedAt(), c.getRevokedAt()))
                .orElse(new ConsentStatusResponse(false, null, null, null));
    }

    @Override
    @Transactional
    public ConsentStatusResponse revokeConsent(Long userId) {
        UserConsent consent = userConsentRepository
                .findFirstByUserIdAndTypeOrderByIdDesc(userId, ConsentType.PRIVACY_AND_TERMS)
                .orElseThrow(() -> new BusinessException("Nenhum consentimento registrado para revogar."));
        if (consent.getRevokedAt() == null) {
            consent.setRevokedAt(Instant.now());
            userConsentRepository.save(consent);
        }
        return new ConsentStatusResponse(
                false, consent.getDocumentVersion(), consent.getAcceptedAt(), consent.getRevokedAt());
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

    /** Valida telefone (11 dígitos) e e-mail; lança BusinessException se inválido. */
    private void validateContact(String phone, String email) {
        if (!DocumentValidations.isValidPhone(phone)) {
            throw new BusinessException("Telefone inválido. Informe DDD + número (11 dígitos).");
        }
        if (!DocumentValidations.isValidEmail(email)) {
            throw new BusinessException("E-mail inválido. Ex.: nome@exemplo.com.");
        }
    }

    /** Bloqueia o cadastro sem o aceite explícito da Política de Privacidade + Termos (LGPD). */
    private void requireConsent(boolean accepted) {
        if (!accepted) {
            throw new BusinessException(
                    "É necessário ler e aceitar a Política de Privacidade e os Termos de Uso para criar a conta.");
        }
    }

    /** Grava o consentimento (versão + data/hora) para que seja demonstrável (LGPD). */
    private void recordConsent(User user) {
        UserConsent consent = new UserConsent();
        consent.setUser(user);
        consent.setType(ConsentType.PRIVACY_AND_TERMS);
        consent.setDocumentVersion(LegalDocuments.PRIVACY_TERMS_VERSION);
        consent.setAcceptedAt(Instant.now());
        userConsentRepository.save(consent);
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
        UserResponse userResponse = UserResponse.from(user, lastSeenOnboardingVersion(user.getId()));
        return AuthResponse.of(token, jwtService.getExpirationMillis(), userResponse);
    }

    private int lastSeenOnboardingVersion(Long userId) {
        return onboardingProgressRepository.findByUserId(userId)
                .map(OnboardingProgress::getLastSeenVersion)
                .orElse(0);
    }
}
