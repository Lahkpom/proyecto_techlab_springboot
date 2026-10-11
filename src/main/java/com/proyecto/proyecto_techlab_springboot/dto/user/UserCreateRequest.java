package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres.")
        String name,

        @NotBlank(message = "El email es obligatoria.")
        @Size(max = 255, message = "El email no puede exceder los 255 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 255, message = "La contraseña no puede exceder los 255 caracteres.")
        String password,

        @Size(max = 50, message = "El teléfono no puede exceder los 50 caracteres.")
        String phone
) {}