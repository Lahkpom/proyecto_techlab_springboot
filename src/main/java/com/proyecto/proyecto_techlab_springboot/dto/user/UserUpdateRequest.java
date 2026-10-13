package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres.")
        String name,

        @Pattern(
                regexp = "^\\d{10}$",
                message = "El teléfono debe contener exactamente 10 dígitos numéricos."
        )
        String phone
) {}