package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.auth.AuthResponse;
import com.proyecto.proyecto_techlab_springboot.dto.auth.LoginRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.UserCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;
import com.proyecto.proyecto_techlab_springboot.exception.auth.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.exception.user.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

        return mapToResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Users user = this.iUserRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("{ email: %s }".formatted(email)));

        if (!user.isEmailVerified()){
            // TODO Hacerle una exception específica para este
            throw new IllegalArgumentException("El Email aún no ha sido verificado.");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(Instant.now())) {
            // TODO Hacerle una exception específica para este
            throw new IllegalArgumentException("La cuenta está bloqueada temporalmente.");
        }

        if (!passwordEncoder.matches(request.password(), user.getUserPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            throw new InvalidCredentialsException("Contraseña Incorrecta.");
        }

        // Actualizamos último login y reseteamos fallos
        user.setLastLoginAt(Instant.now());
        user.setFailedLoginAttempts(0);

        // Simulamos token hasta implementar JwtService
        String mockToken = "temp-jwt-token-for-%s".formatted(user.getUserId());

        return AuthResponse.of(mockToken, mapToResponse(user));
    }





//    Autenticación: El backend busca al usuario por email, valida que user.isEmailVerified sea true, que no esté bloqueado y compara hashes con passwordEncoder.matches(rawPassword, encodedPassword).
//Firma del JWT: Si todo coincide, el backend genera una cadena compacta (JWT) firmada criptográficamente con una clave secreta (HMAC-SHA256). Dentro del token viajan datos públicos (claims): el ID del usuario, su email, su rol y la fecha de expiración.

    private UserResponse mapToResponse(Users entity) {
        return new UserResponse(
                entity.getUserId(),
                entity.getUserRole(),
                entity.getUserName(),
                entity.getUserEmail(),
                entity.getUserPhone(),
                entity.getCompanyName(),
                entity.getLastLoginAt()
        );
    }

    private void validateUserEmailOrThrow(String email) {
        if (this.iUserRepository.findByUserEmailIgnoreCase(email).isPresent())
            throw new UserAlreadyExistsException("{ email: %s }".formatted(email));
    }
}
