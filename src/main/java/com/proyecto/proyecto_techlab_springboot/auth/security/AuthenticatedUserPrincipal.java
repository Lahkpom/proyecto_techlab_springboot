package com.proyecto.proyecto_techlab_springboot.auth.security;

import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;

import java.util.UUID;

public record AuthenticatedUserPrincipal(UUID userId, String email, UserRole role) {}
