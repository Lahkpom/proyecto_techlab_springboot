package com.proyecto.proyecto_techlab_springboot.dto.color;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ColorCreateRequest(
        @NotBlank(message = "El nombre del color es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String name,

        @NotBlank(message = "La descripción del color es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String desc,

        @NotBlank(message = "El código HEX es obligatorio")
        @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "El código de color debe tener formato HEX válido (ej. #FF00FF)")
        String code
) {}