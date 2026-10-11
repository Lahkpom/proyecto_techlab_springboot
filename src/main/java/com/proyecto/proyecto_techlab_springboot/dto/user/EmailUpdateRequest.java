package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailUpdateRequest(
        @NotNull(message = "El email es obligatorio")
        @Size(max = 255, message = "El email no puede exceder los 255 caracteres.")
        String password
) {}