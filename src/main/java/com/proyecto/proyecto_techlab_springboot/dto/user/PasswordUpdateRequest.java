package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotNull(message = "La contraseña es obligatoria")
        @Size(max = 255, message = "La contraseña no puede exceder los 255 caracteres.")
        String password
) {}