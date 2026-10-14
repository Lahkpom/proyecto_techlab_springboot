package com.proyecto.proyecto_techlab_springboot.dto.auth;

import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;

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
