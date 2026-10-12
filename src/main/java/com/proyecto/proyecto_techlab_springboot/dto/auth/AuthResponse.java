package com.proyecto.proyecto_techlab_springboot.dto.auth;

import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;

public record AuthResponse(
        String          accessToken,
        String          tokenType,
        UserResponse    user
) {
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}