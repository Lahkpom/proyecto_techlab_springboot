package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres.")
        String name,

        @Size(max = 50, message = "El teléfono no puede exceder los 50 caracteres.")
        String phone
) {}