package com.proyecto.proyecto_techlab_springboot.user.dto;

import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotNull(message = "El rol del usuario es obligatorio.")
        UserRole userRole,

        @Size(max = 255, message = "La razón social no puede superar los 255 caracteres.")
        String companyName,

        TaxIdType taxIdType,

        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres.")
        String taxIdNumber
) {}
