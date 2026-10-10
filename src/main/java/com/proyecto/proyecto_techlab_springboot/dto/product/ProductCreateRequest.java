package com.proyecto.proyecto_techlab_springboot.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 255, message = "El nombre no puede exceder los 255 caracteres.")
        String name,

        @NotBlank(message = "La descripción corta es obligatoria.")
        @Size(max = 255, message = "La descripción corta no puede exceder los 255 caracteres.")
        String shortDesc,

        @NotBlank(message = "La descripción larga es obligatoria.")
        @Size(max = 5000, message = "La descripción larga no puede exceder los 5000 caracteres.")
        String longDesc
) {}