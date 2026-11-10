package com.proyecto.proyecto_techlab_springboot.auth.dto;

import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;

import java.time.Instant;

public record AuthResponse(
        String          accessToken,
        String          refreshToken,
        String          tokenType,
        Instant         expiresAt,
        UserResponse    user
) {
    public static AuthResponse of(
            String          accessToken,
            String          refreshToken,
            Instant         expiresAt,
            UserResponse    user
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresAt,
                user
        );
    }
}
