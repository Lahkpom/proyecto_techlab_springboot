package com.proyecto.proyecto_techlab_springboot.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio.")
        String refreshToken
) {}