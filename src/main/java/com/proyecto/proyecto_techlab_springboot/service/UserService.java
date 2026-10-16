package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.common.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.*;
import com.proyecto.proyecto_techlab_springboot.exception.user.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        user.setUserName(request.name());
        user.setUserPhone(request.phone());

        log.info("Usuario modificado exitosamente { ID: {}, email: {}, name: {}, phone: {} }", user.getUserId(), user.getUserEmail(), user.getUserName(), user.getUserPhone());

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserPassword(UUID id, PasswordUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        user.setUserPassword(passwordEncoder.encode(request.password()));

        log.info("La contraseña fue actualizada correctamente { ID: {} }",  user.getUserId());

        return mapToResponse(user);
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
                entity.getLastLoginAt(),
                entity.getPasswordExpiresAt()
        );
    }

    public Users findUserByIdOrThrow(UUID id) {
        return this.iUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("{ ID: %s }".formatted(id)));
    }
}

