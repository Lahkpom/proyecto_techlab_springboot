package com.proyecto.proyecto_techlab_springboot.user.service;

import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.exception.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.dto.AdminUserUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.PasswordUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserAdminSegment;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.exception.InvalidUserDataException;
import com.proyecto.proyecto_techlab_springboot.user.exception.UserAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.user.exception.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Users createAccount(Users user) {
        if (iUserRepository.findByUserEmailIgnoreCase(user.getUserEmail()).isPresent()) {
            throw new UserAlreadyExistsException("{ email: %s }".formatted(user.getUserEmail()));
        }
        return iUserRepository.save(user);
    }

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
    public void updateUserTaxData(UUID userId, TaxIdType taxIdType, String taxIdNumber) {
        Users user = this.findUserByIdOrThrow(userId);
        user.setTaxIdType(taxIdType);
        user.setTaxIdNumber(taxIdNumber);
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
        user.setMustChangePassword(false);
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
    public UserResponse updateUserStatus(
            UUID id,
            StatusUpdateRequest request,
            AuthenticatedUserPrincipal actor
    ) {
        Users user = this.findUserByIdOrThrow(id);
        UserRole targetRole = user.getUserRole();

        if (actor.role() != UserRole.ADMIN && actor.role() != UserRole.OPERATOR) {
            throw new AccessDeniedException("No tiene permisos para modificar el estado de usuarios.");
        }
        if (targetRole == UserRole.ADMIN || user.getUserId().equals(actor.userId())) {
            throw new AccessDeniedException("No se puede modificar a un administrador ni al usuario autenticado.");
        }
        if (actor.role() == UserRole.OPERATOR
                && (!isCustomerOrWholesaler(targetRole) || request.rowStatus() == RowStatus.DELETED)) {
            throw new AccessDeniedException(
                    "Los operadores solo pueden cambiar el estado de clientes y mayoristas sin eliminarlos."
            );
        }
        if (request.rowStatus() == RowStatus.DELETED && actor.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede eliminar registros de forma lógica.");
        }

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

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsersAdmin(
            AuthenticatedUserPrincipal actor,
            UserAdminSegment segment,
            UserRole roleFilter,
            RowStatus rowStatus,
            String search,
            Pageable pageable
    ) {
        List<UserRole> allowedRoles;
        if (actor.role() == UserRole.OPERATOR) {
            if (segment == UserAdminSegment.OPERATORS) {
                throw new AccessDeniedException(
                        "Los operadores solo pueden consultar clientes y mayoristas."
                );
            }
            allowedRoles = List.of(UserRole.CUSTOMER, UserRole.WHOLESALER);
        } else if (actor.role() == UserRole.ADMIN) {
            allowedRoles = segment == UserAdminSegment.OPERATORS
                    ? List.of(UserRole.OPERATOR)
                    : List.of(UserRole.CUSTOMER, UserRole.WHOLESALER);
        } else {
            throw new AccessDeniedException(
                    "No tiene permisos para consultar usuarios administrativos."
            );
        }

        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return iUserRepository.searchUsersAdmin(
                        allowedRoles,
                        roleFilter,
                        rowStatus,
                        normalizedSearch,
                        pageable
                )
                .map(this::mapToResponse);
    }

    @Transactional
    public UserResponse updateAdminUser(
            UUID id,
            AdminUserUpdateRequest request,
            AuthenticatedUserPrincipal actor
    ) {
        if (actor.role() != UserRole.OPERATOR && actor.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("No tiene permisos para modificar usuarios administrativos.");
        }

        Users user = findUserByIdOrThrow(id);
        UserRole currentRole = user.getUserRole();

        if (currentRole == UserRole.ADMIN || user.getUserId().equals(actor.userId())) {
            throw new AccessDeniedException(
                    "No se puede modificar a un administrador ni al usuario autenticado."
            );
        }

        if (actor.role() == UserRole.OPERATOR
                && (currentRole == UserRole.OPERATOR || currentRole == UserRole.ADMIN)) {
            throw new AccessDeniedException(
                    "Los operadores solo pueden modificar clientes y mayoristas."
            );
        }

        UserRole targetRole = request.userRole();
        if (!isCustomerOrWholesaler(currentRole) || !isCustomerOrWholesaler(targetRole)) {
            throw new AccessDeniedException(
                    "Solo se permiten cambios entre clientes y mayoristas."
            );
        }

        String taxIdNumber = normalizeOptionalValue(request.taxIdNumber());
        if (targetRole == UserRole.WHOLESALER
                && (request.taxIdType() != TaxIdType.CUIT
                || taxIdNumber == null
                || !taxIdNumber.matches("^\\d{11}$"))) {
            throw new InvalidUserDataException(
                    "Para registrar un mayorista se requiere un CUIT válido de 11 dígitos."
            );
        }

        user.setUserRole(targetRole);
        user.setCompanyName(normalizeOptionalValue(request.companyName()));
        user.setTaxIdType(request.taxIdType());
        user.setTaxIdNumber(taxIdNumber);

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateAdminUserStatus(
            UUID id,
            StatusUpdateRequest request,
            AuthenticatedUserPrincipal actor
    ) {
        return updateUserStatus(id, request, actor);
    }

    private boolean isCustomerOrWholesaler(UserRole role) {
        return role == UserRole.CUSTOMER || role == UserRole.WHOLESALER;
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
                entity.getPasswordExpiresAt(),
                entity.getRowStatus()
        );
    }

    public Optional<Users> findUserById(UUID id) {
        return this.iUserRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Users> findUserByEmail(String email) {
        return iUserRepository.findByUserEmailIgnoreCase(email);
    }

    public Users findUserByIdOrThrow(UUID id) {
        return this.findUserById(id)
                .orElseThrow(() -> new UserNotFoundException("{ ID: %s }".formatted(id)));
    }
}
