package com.proyecto.proyecto_techlab_springboot.auth.service;

import com.proyecto.proyecto_techlab_springboot.auth.dto.*;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.auth.exception.AccountLockedException;
import com.proyecto.proyecto_techlab_springboot.auth.exception.EmailNotVerifiedException;
import com.proyecto.proyecto_techlab_springboot.auth.exception.ExpiredPasswordException;
import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.user.exception.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.auth.model.UserSession;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.repository.IUserRepository;
import com.proyecto.proyecto_techlab_springboot.auth.repository.IUserSessionRepository;
import com.proyecto.proyecto_techlab_springboot.auth.util.TokenHashUtil;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final IUserRepository           iUserRepository;
    private final IUserSessionRepository    iUserSessionRepository;
    private final UserService               userService;
    private final PasswordEncoder           passwordEncoder;

    private final JwtService jwtService;
    private final LoginAuditService auditService;

    private static final int MAX_FAILED_ATTEMPTS    = 5;
    private static final int REFRESH_TOKEN_DAYS     = 7;

    @Transactional(readOnly = true)
    public UserResponse getAuthenticatedUserProfile(UUID userId) {
        return userService.getUserById(userId);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest req) {
        return registerUser(
                UserRole.CUSTOMER,
                req.name(),
                req.email(),
                req.password(),
                req.phone(),
                req.taxIdType(),
                req.taxIdNumber(),
                null
        );
    }

    @Transactional
    public UserResponse createWholesaler(WholesalerCreateRequest req) {
        return registerUser(
                UserRole.WHOLESALER,
                req.name(),
                req.email(),
                req.password(),
                req.phone(),
                req.taxIdType(),
                req.taxIdNumber(),
                req.companyName()
        );
    }

    // TODO - Hacer las validaciones correspondientes al tipo y número de identificación fiscal acorde al tipo de usuario registrado
    private UserResponse registerUser(
            UserRole    role,
            String      rawName,
            String      rawEmail,
            String      rawPassword,
            String      rawPhone,
            TaxIdType   taxIdType,
            String      rawTaxIdNumber,
            String      rawCompanyName
    ) {
        String name        = rawName != null ? rawName.trim() : null;
        String email       = rawEmail.trim().toLowerCase();
        String password    = passwordEncoder.encode(rawPassword);
        String phone       = rawPhone != null ? rawPhone.trim() : null;
        String taxIdNumber = rawTaxIdNumber != null ? rawTaxIdNumber.trim() : null;
        String company     = rawCompanyName != null ? rawCompanyName.trim() : null;

        validateUserEmailOrThrow(email);

        Users user = Users.builder()
                .userRole(role)
                .userName(name)
                .userEmail(email)
                .userPassword(password)
                .userPhone(phone)
                .companyName(company)
                .taxIdType(taxIdType)
                .taxIdNumber(taxIdNumber)
                .isEmailVerified(true)
                .build();

        Users saved = this.iUserRepository.save(user);

        log.info("Usuario {} creado exitosamente { ID: {}, email: {}, compañía: {} }", 
                    role, saved.getUserId(), saved.getUserEmail(), saved.getCompanyName());

        return mapToResponse(saved);
    }

    // Evitar el rollback cuando salten estas exception para que borre los insert en la tabla
    @Transactional(noRollbackFor = {
            InvalidCredentialsException.class,
            AccountLockedException.class,
            EmailNotVerifiedException.class,
            ExpiredPasswordException.class
    })
    public AuthResponse login(LoginRequest req, HttpServletRequest httpRequest) {
        String  ip          = extractClientIp(httpRequest);
        String  userAgent   = httpRequest.getHeader("User-Agent");
        String  email       = req.email().trim().toLowerCase();
        Users   user        = this.getUserByEmail(email).orElse(null);

        // Usuario inexistente
        if (user == null) {
            auditService.recordAttempt(
                    null,
                    req.email(),
                    false,
                    "USER_NOT_FOUND",
                    ip,
                    userAgent
            );

            throw new InvalidCredentialsException("{ email: %s }".formatted(email));
        }

        // Comprobar email verificado
        if (!user.isEmailVerified()) {
            auditService.recordAttempt(
                    user.getUserId(),
                    req.email(),
                    false,
                    "EMAIL_NOT_VERIFIED",
                    ip,
                    userAgent
            );

            throw new EmailNotVerifiedException();
        }


        // Comprobar bloqueo de cuenta
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(Instant.now())) {
                auditService.recordAttempt(
                        user.getUserId(),
                        req.email(),
                        false,
                        "ACCOUNT_LOCKED",
                        ip,
                        userAgent
                );

                throw new AccountLockedException(user.getLockedUntil().toString());
            } else {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
            }
        }

        // Comprobar contraseña
        if (!passwordEncoder.matches(req.password(), user.getUserPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            log.warn("Intento de login fallido #{} para email: {} (userId: {})", attempts, user.getUserEmail(), user.getUserId());

            if (attempts >= MAX_FAILED_ATTEMPTS) { // Bloquear por 15 minutos
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));

                log.warn(
                        "Se impone bloqueo de cuenta al email {} hasta {} por {} intentos fallidos de login (userId: {})",
                        user.getUserEmail(),
                        user.getFailedLoginAttempts(),
                        attempts,
                        user.getUserId()
                );

                auditService.recordAttempt(
                        user.getUserId(),
                        req.email(),
                        false,
                        "ACCOUNT_LOCKED",
                        ip,
                        userAgent
                );

                throw new AccountLockedException(user.getLockedUntil().toString());
            }

            auditService.recordAttempt(
                    user.getUserId(),
                    req.email(),
                    false,
                    "INVALID_CREDENTIALS",
                    ip,
                    userAgent
            );

            throw new InvalidCredentialsException("Contraseña Incorrecta.");
        }

        if (user.getPasswordExpiresAt().isBefore(Instant.now())) {
            auditService.recordAttempt(
                    user.getUserId(),
                    req.email(),
                    false,
                    "PASSWORD_EXPIRED",
                    ip,
                    userAgent
            );

            throw new ExpiredPasswordException();
        }

        // Crear sesión y tokens antes de registrar el login como exitoso.
        AuthResponse authResponse = createSessionAndTokens(user, ip, userAgent).authResponse();

        auditService.recordAttempt(
            user.getUserId(),
            req.email(),
            true,
            null,
            ip,
            userAgent
        );

        // El trigger Postgres actualiza last_login_at y limpia failed_login_attempts.
        log.info("Usuario inició sesión exitosamente { email: {}, ID: {} }", user.getUserEmail(), user.getUserId());
        return authResponse;
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResponse refreshToken(RefreshTokenRequest req, HttpServletRequest httpRequest) {
        String tokenHash = TokenHashUtil.hash(req.refreshToken());

        UserSession session = this.iUserSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Sesión inválida o revocada."));

        if (session.isRevoked()) {
            UUID userId = session.getUser().getUserId();
            log.warn("Posible robo o reutilización de Refresh Token revocado para el usuario {}", userId);
            this.iUserSessionRepository.revokeAllActiveSessionsByUserId(userId);
            throw new InvalidCredentialsException(
                    "Se detectó reutilización de sesión revocada. Todas las sesiones han sido cerradas por seguridad."
            );
        }

        // Comprobar expiración del refresh token
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setRevoked(true);
            this.iUserSessionRepository.save(session);
            throw new InvalidCredentialsException("El refresh token ha expirado. Inicie sesión nuevamente.");
        }

        // Emitir nuevo par de tokens
        Users user          = session.getUser();
        String ip           = extractClientIp(httpRequest);
        String userAgent    = httpRequest.getHeader("User-Agent");

        SessionTokens newSession = createSessionAndTokens(user, ip, userAgent);

        session.setReplacedBySessionId(newSession.session().getSessionId());
        session.setRevoked(true);
        this.iUserSessionRepository.saveAndFlush(session);

        return newSession.authResponse();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = TokenHashUtil.hash(rawRefreshToken);
            this.iUserSessionRepository.findByRefreshTokenHashAndIsRevokedFalse(tokenHash)
                    .ifPresent(session -> {
                        session.setRevoked(true);
                        this.iUserSessionRepository.save(session);
                    });
        }
    }

    private String extractClientIp(HttpServletRequest req) {
        String xfHeader = req.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) return req.getRemoteAddr();

        return xfHeader.split(",")[0].trim();
    }

    private SessionTokens createSessionAndTokens(Users user, String ip, String userAgent) {
        String  accessToken     = jwtService.generateAccessToken(user);
        String  rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        Instant expiresAt       = Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS);

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(TokenHashUtil.hash(rawRefreshToken))
                .deviceInfo(userAgent != null && userAgent.length() > 250 ? userAgent.substring(0, 250) : userAgent)
                .ipAddress(ip)
                .isRevoked(false)
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        UserSession savedSession = this.iUserSessionRepository.save(session);

        AuthResponse authResponse = AuthResponse.of(accessToken, rawRefreshToken, expiresAt, mapToResponse(user));
        return new SessionTokens(savedSession, authResponse);
    }

    private record SessionTokens(UserSession session, AuthResponse authResponse) {}

    private UserResponse mapToResponse(Users entity) {
        return new UserResponse(
                entity.getUserId(),
                entity.getUserRole(),
                entity.getUserName(),
                entity.getUserEmail(),
                entity.getUserPhone(),
                entity.getCompanyName(),
                entity.getTaxIdType(),
                entity.getTaxIdNumber(),
                entity.getLastLoginAt(),
                entity.getPasswordExpiresAt()
        );
    }

    private void validateUserEmailOrThrow(String email) {
        if (this.getUserByEmail(email).isPresent())
            throw new UserAlreadyExistsException("{ email: %s }".formatted(email));
    }

    private Optional<Users> getUserByEmail(String email) {
        return this.iUserRepository.findByUserEmailIgnoreCase(email);
    }
}
