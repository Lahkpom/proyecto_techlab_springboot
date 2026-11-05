package com.proyecto.proyecto_techlab_springboot.dto.auth;

import com.proyecto.proyecto_techlab_springboot.model.enums.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.util.ValidationUtil;
import jakarta.validation.constraints.*;

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

        @Pattern(
                regexp = ValidationUtil.PHONE_REGEX,
                message = "El teléfono debe contener exactamente 10 dígitos numéricos."
        )
        String phone,

        @NotBlank(message = "El nombre de la compañía es obligatorio.")
        @Size(max = 255, message = "El nombre de la compañía no puede exceder los 255 caracteres.")
        String companyName,

        @NotNull(message = "El tipo de documento de identificación fiscal no puede ser nulo.")
        TaxIdType taxIdType,

        @NotBlank(message = "El número de identificación fiscal no puede estar vacío.")
        String taxIdNumber
) {}