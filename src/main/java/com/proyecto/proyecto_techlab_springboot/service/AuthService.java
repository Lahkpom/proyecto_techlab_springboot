package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.auth.UserCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.UserResponse;
import com.proyecto.proyecto_techlab_springboot.model.Users;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
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
}
