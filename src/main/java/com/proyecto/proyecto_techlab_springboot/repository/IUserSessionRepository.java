package com.proyecto.proyecto_techlab_springboot.repository;

import com.proyecto.proyecto_techlab_springboot.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserSessionRepository  extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHashAndIsRevokedFalse(String refreshTokenHash);

    void deleteAllByUser_UserId(UUID userId);
}
