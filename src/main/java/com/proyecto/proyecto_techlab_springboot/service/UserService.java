package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.*;
import com.proyecto.proyecto_techlab_springboot.exception.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
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
        Users user = Users.builder()
                .userName(request.name())
                .userEmail(request.email())
                .userPassword(passwordEncoder.encode(request.password()))
                .userPhone(request.phone())
                .companyName(request.companyName())
                .build();

        Users saved = this.iUserRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return mapToResponse(this.findUserByIdOrThrow(id));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        user.setUserName(request.name());
        user.setUserPhone(request.phone());

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserPassword(UUID id, PasswordUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        user.setUserPassword(passwordEncoder.encode(request.password()));

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID id, StatusUpdateRequest request) {
        Users user = this.findUserByIdOrThrow(id);

        user.setRowStatus(request.rowStatus());

        if (request.rowStatus() == RowStatus.DELETED) {
            user.setUserEmail("deleted_" + user.getUserId() + "@anonymized.local");
            user.setUserName("Usuario Eliminado");
            user.setUserPhone(null);
            user.setUserPassword("ANONYMIZED");
        }

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
                entity.getLastLoginAt()
        );
    }

    private Users findUserByIdOrThrow(UUID id) {
        return this.iUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("{ ID: %s }".formatted(id)));
    }
}

