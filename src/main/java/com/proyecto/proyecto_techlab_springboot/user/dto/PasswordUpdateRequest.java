package com.proyecto.proyecto_techlab_springboot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 255, message = "La nueva contraseña debe tener entre 8 y 255 caracteres.")
        String newPassword
) {}