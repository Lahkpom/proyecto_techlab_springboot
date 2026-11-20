package com.proyecto.proyecto_techlab_springboot.auth.repository;

import com.proyecto.proyecto_techlab_springboot.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserSessionRepository  extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSession> findByRefreshTokenHashAndIsRevokedFalse(String refreshTokenHash);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.user.userId = :userId AND s.isRevoked = false")
    void revokeAllActiveSessionsByUserId(@Param("userId") UUID userId);

    void deleteAllByUser_UserId(UUID userId);
}
