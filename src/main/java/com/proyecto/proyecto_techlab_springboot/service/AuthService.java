package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.auth.*;
import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;
import com.proyecto.proyecto_techlab_springboot.exception.auth.AccountLockedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.EmailNotVerifiedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.ExpiredPasswordException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.exception.user.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.model.UserSession;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import com.proyecto.proyecto_techlab_springboot.repository.IUserSessionRepository;
import com.proyecto.proyecto_techlab_springboot.util.TokenHashUtil;
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
    private final PasswordEncoder           passwordEncoder;

    private final JwtService        jwtService;
    private final LoginAuditService auditService;

    private static final int MAX_FAILED_ATTEMPTS    = 5;
    private static final int REFRESH_TOKEN_DAYS     = 7;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String name     = request.name().trim();
        String email    = request.email().trim().toLowerCase();
        String password = passwordEncoder.encode(request.password());
        String phone    = request.phone() != null ? request.phone().trim() : null;

        validateUserEmailOrThrow(email);

        Users user = Users.builder()
                .userRole(UserRole.CUSTOMER)
                .userName(name)
                .userEmail(email)
                .userPassword(password)
                .userPhone(phone)
                .build();

        Users saved = this.iUserRepository.save(user);

        log.info("Usuario CUSTOMER creado exitosamente { email: {}, ID: {} }", saved.getUserEmail(), saved.getUserId());
        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse createWholesaler(WholesalerCreateRequest request) {
        String name     = request.name().trim();
        String email    = request.email().trim().toLowerCase();
        String password = passwordEncoder.encode(request.password());
        String phone    = request.phone() != null ? request.phone().trim() : null;
        String company  = request.companyName().trim();

        validateUserEmailOrThrow(email);

        Users user = Users.builder()
                .userRole(UserRole.WHOLESALE)
                .userName(name)
                .userEmail(email)
                .userPassword(password)
                .userPhone(phone)
                .companyName(company)
                .build();

        Users saved = this.iUserRepository.save(user);

        log.info("Usuario WHOLESALER creado exitosamente { compañía: {}, email: {}, ID: {} }", saved.getCompanyName(), saved.getUserEmail(), saved.getUserId());
        return mapToResponse(saved);
    }

    // Evitar el rollback cuando salten estas exception para que borre los insert en la tabla
    @Transactional(noRollbackFor = {
            InvalidCredentialsException.class,
            AccountLockedException.class,
            EmailNotVerifiedException.class,
            ExpiredPasswordException.class
    })
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String  ip          = extractClientIp(httpRequest);
        String  userAgent   = httpRequest.getHeader("User-Agent");
        String  email       = request.email().trim().toLowerCase();
        Users   user        = this.getUserByEmail(email).orElse(null);

        // Usuario inexistente
        if (user == null) {
            auditService.recordAttempt(
                    null,
                    request.email(),
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
                    request.email(),
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
                        request.email(),
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
        if (!passwordEncoder.matches(request.password(), user.getUserPassword())) {
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
                        request.email(),
                        false,
                        "ACCOUNT_LOCKED",
                        ip,
                        userAgent
                );

                throw new AccountLockedException(user.getLockedUntil().toString());
            }

            auditService.recordAttempt(
                    user.getUserId(),
                    request.email(),
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
                    request.email(),
                    false,
                    "PASSWORD_EXPIRED",
                    ip,
                    userAgent
            );

            throw new ExpiredPasswordException();
        }

        // Login exitoso -> El trigger Postgres actualiza last_login_at y limpia failed_login_attempts
        log.info("Usuario inició sesión exitosamente { email: {}, ID: {} }", user.getUserEmail(), user.getUserId());

        auditService.recordAttempt(
                user.getUserId(),
                request.email(),
                true,
                null,
                ip,
                userAgent
        );

        // Emitir tokens y crear sesión
        return createSessionAndTokens(user, ip, userAgent);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenHash = TokenHashUtil.hash(request.refreshToken());

        UserSession session = this.iUserSessionRepository.findByRefreshTokenHashAndIsRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Sesión inválida o revocada."));

        // Comprobar expiración del refresh token
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setRevoked(true);
            this.iUserSessionRepository.save(session);
            throw new InvalidCredentialsException("El refresh token ha expirado. Inicie sesión nuevamente.");
        }

        // ROTACIÓN DE REFRESH TOKEN (Se revoca el anterior)
        session.setRevoked(true);

        // Emitir nuevo par de tokens
        Users user          = session.getUser();
        String ip           = extractClientIp(httpRequest);
        String userAgent    = httpRequest.getHeader("User-Agent");

        AuthResponse newAuth = createSessionAndTokens(user, ip, userAgent);

        // Enlazar sesión vieja con la nueva para trazabilidad
        this.iUserSessionRepository.save(session);

        return newAuth;
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

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) return request.getRemoteAddr();

        return xfHeader.split(",")[0].trim();
    }

    private AuthResponse createSessionAndTokens(Users user, String ip, String userAgent) {
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

        this.iUserSessionRepository.save(session);

        return AuthResponse.of(accessToken, rawRefreshToken, expiresAt, mapToResponse(user));
    }

    private UserResponse mapToResponse(Users entity) {
        return new UserResponse(
                entity.getUserId(),
                entity.getUserRole(),
                entity.getUserName(),
                entity.getUserEmail(),
                entity.getUserPhone(),
                entity.getCompanyName(),
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
