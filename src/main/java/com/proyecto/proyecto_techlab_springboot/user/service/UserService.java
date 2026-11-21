package com.proyecto.proyecto_techlab_springboot.user.service;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.user.exception.InvalidUserDataException;
import com.proyecto.proyecto_techlab_springboot.user.exception.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.repository.IUserRepository;
import com.proyecto.proyecto_techlab_springboot.user.dto.PasswordUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserUpdateRequest;
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
public class UserService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return mapToResponse(this.findUserByIdOrThrow(id));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        String normalizedName = request.name().trim();
        String normalizedPhone = normalizeOptionalValue(request.phone());
        String normalizedTaxIdNumber = normalizeOptionalValue(request.taxIdNumber());

        if ((request.taxIdType() != null && normalizedTaxIdNumber == null)
                || (request.taxIdType() == null && normalizedTaxIdNumber != null)) {
            throw new InvalidUserDataException("Debe completar tanto el tipo como el número de documento.");
        }

        if (request.taxIdType() != null && !isValidTaxIdNumber(request.taxIdType(), normalizedTaxIdNumber)) {
            throw new InvalidUserDataException(
                    "El número de documento no tiene un formato válido para " + request.taxIdType() + "."
            );
        }

        user.setUserName(normalizedName);
        user.setUserPhone(normalizedPhone);
        user.setTaxIdType(request.taxIdType());
        user.setTaxIdNumber(normalizedTaxIdNumber);

        log.info("Usuario modificado exitosamente { ID: {}, email: {}, name: {}, phone: {} }", user.getUserId(), user.getUserEmail(), user.getUserName(), user.getUserPhone());

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserPassword(UUID id, PasswordUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getUserPassword())) {
            throw new InvalidCredentialsException("La contraseña actual ingresada es incorrecta.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getUserPassword())) {
            throw new InvalidUserDataException("La nueva contraseña no puede ser igual a la contraseña actual.");
        }

        user.setUserPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordExpiresAt(Instant.now().plus(180, ChronoUnit.DAYS));

        log.info("La contraseña fue actualizada correctamente { ID: {} }",  user.getUserId());

        return mapToResponse(user);
    }

    private String normalizeOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isValidTaxIdNumber(TaxIdType taxIdType, String taxIdNumber) {
        String regex = switch (taxIdType) {
            case DNI -> "^\\d{7,8}$";
            case CUIL, CUIT -> "^\\d{11}$";
            case PASAPORTE -> "^[A-Za-z0-9]{6,15}$";
        };
        return taxIdNumber.matches(regex);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID id, StatusUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);
        RowStatus lastStatus = user.getRowStatus();
        user.setRowStatus(request.rowStatus());

        if (request.rowStatus() == RowStatus.DELETED) {
            user.setUserEmail("deleted_" + user.getUserId() + "@anonymized.local");
            user.setUserName("Usuario Eliminado");
            user.setUserPhone(null);
            user.setUserPassword("ANONYMIZED");
        }

        log.info("Estado del Usuario modificado exitosamente { ID: {}, Estado Anterior: {}, Nuevo Estado: {} }", user.getUserId(), lastStatus, user.getRowStatus());

        return mapToResponse(user);
    }

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

    public Optional<Users> findUserById(UUID id) {
        return this.iUserRepository.findById(id);
    }

    public Users findUserByIdOrThrow(UUID id) {
        return this.findUserById(id)
                .orElseThrow(() -> new UserNotFoundException("{ ID: %s }".formatted(id)));
    }
}
