package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.auth.AuthResponse;
import com.proyecto.proyecto_techlab_springboot.dto.auth.LoginRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.UserCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;
import com.proyecto.proyecto_techlab_springboot.exception.auth.AccountLockedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.EmailNotVerifiedException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.ExpiredPasswordException;
import com.proyecto.proyecto_techlab_springboot.exception.auth.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.exception.user.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;
    // Más adelante inyectarás: private final JwtService jwtService;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String name     = request.name().trim();
        String email    = request.email().trim().toLowerCase();
        String password = passwordEncoder.encode(request.password());
        String phone    = request.phone() != null ? request.phone().trim() : null;

        validateUserEmailOrThrow(email);
        // TODO: Validar también el formato del teléfono

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
        // TODO: Validar también el formato del teléfono

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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Users user = this.getUserByEmail(email);

        if (!user.isEmailVerified()) throw new EmailNotVerifiedException();

        if (user.getPasswordExpiresAt().isBefore(Instant.now())) throw new ExpiredPasswordException();

        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(Instant.now()))
                throw new AccountLockedException(user.getLockedUntil().toString());

            // Si ya expiró el castigo, reseteamos el bloqueo
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.password(), user.getUserPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            log.warn("Intento de login fallido #{} para email: {} (userId: {})", attempts, user.getUserEmail(), user.getUserId());

            if (attempts >= 5) { // Bloquear por 15 minutos
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
                log.warn(
                        "Se impone bloqueo de cuenta al email {} hasta {} por {} intentos fallidos de login (userId: {})",
                        user.getUserEmail(),
                        user.getFailedLoginAttempts(),
                        attempts,
                        user.getUserId()
                );

                throw new AccountLockedException(user.getLockedUntil().toString());
            }

            throw new InvalidCredentialsException("Contraseña Incorrecta.");
        }

        // Actualizamos último login y reseteamos fallos
        user.setLastLoginAt(Instant.now());
        user.setFailedLoginAttempts(0);

        // Simulamos token hasta implementar JwtService
        String mockToken = "temp-jwt-token-for-%s".formatted(user.getUserId());

        log.info("Usuario inició sesión exitosamente { email: {}, ID: {} }", user.getUserEmail(), user.getUserId());
        return AuthResponse.of(mockToken, mapToResponse(user));
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
        if (this.iUserRepository.findByUserEmailIgnoreCase(email).isPresent())
            throw new UserAlreadyExistsException("{ email: %s }".formatted(email));
    }

    private Users getUserByEmail(String email) {
        return this.iUserRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("{ email: %s }".formatted(email)));
    }
}
