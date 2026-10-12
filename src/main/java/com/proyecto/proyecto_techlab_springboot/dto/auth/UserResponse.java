package com.proyecto.proyecto_techlab_springboot.dto.auth;

import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID        userId,
        UserRole    userRole,
        String      userName,
        String      userEmail,
        String      userPhone,
        String      companyName,
        Instant     lastLoginAt
) {}
