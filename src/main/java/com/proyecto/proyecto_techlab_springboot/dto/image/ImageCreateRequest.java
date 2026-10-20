package com.proyecto.proyecto_techlab_springboot.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record ImageCreateRequest(
        @NotBlank(message = "La URL de la imagen es obligatoria.")
        @URL(message = "La URL provista no tiene un formato válido.")
        String imageUrl,

        @NotNull(message = "El indicador de imagen principal es obligatorio.")
        Boolean isPrimary
) {}