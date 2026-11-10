package com.proyecto.proyecto_techlab_springboot.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio.")
        String refreshToken
) {}