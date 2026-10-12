package com.proyecto.proyecto_techlab_springboot.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WholesalerCreateRequest(

        // Nombre del contacto
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres.")
        String name,

        @NotBlank(message = "El email es obligatoria.")
        @Email(message = "El formato de email es inválido.")
        @Size(max = 255, message = "El email no puede exceder los 255 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 255, message = "La contraseña no puede exceder los 255 caracteres.")
        String password,

        @Size(max = 50, message = "El teléfono no puede exceder los 50 caracteres.")
        String phone,

        @NotBlank(message = "El nombre de la compañía es obligatorio.")
        @Size(max = 255, message = "El nombre de la compañía no puede exceder los 255 caracteres.")
        String companyName
) {}