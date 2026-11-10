package com.proyecto.proyecto_techlab_springboot.user.dto;

import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID        userId,
        UserRole    userRole,
        String      userName,
        String      userEmail,
        String      userPhone,
        String      companyName,
        TaxIdType   taxIdType,
        String      taxIdNumber,
        Instant     lastLoginAt,
        Instant     passwordExpiresAt
) {}
