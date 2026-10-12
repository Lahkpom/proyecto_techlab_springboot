package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.auth.UserCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.UserResponse;
import com.proyecto.proyecto_techlab_springboot.exception.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.exception.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        validateUserEmailOrThrow(request.email());

        Users user = Users.builder()
                .userName(request.name())
                .userEmail(request.email())
                .userPassword(passwordEncoder.encode(request.password()))
                .userPhone(request.phone())
                .build();

        Users saved = this.iUserRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse createWholesaler(WholesalerCreateRequest request) {
        validateUserEmailOrThrow(request.email());

        Users user = Users.builder()
                .userRole(UserRole.WHOLESALE)
                .userName(request.name())
                .userEmail(request.email())
                .userPassword(passwordEncoder.encode(request.password()))
                .userPhone(request.phone())
                .companyName(request.companyName())
                .build();

        Users saved = this.iUserRepository.save(user);

        return mapToResponse(saved);
    }

//    findByUserEmail(String email);) para buscar al usuario por mail y validar la clave con passwordEncoder.matches(rawPassword, user.getUserPassword())
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
        if (this.iUserRepository.findByEmail(email).isPresent())
            throw new UserAlreadyExistsException("{ email: %s }".formatted(email));
    }
}
