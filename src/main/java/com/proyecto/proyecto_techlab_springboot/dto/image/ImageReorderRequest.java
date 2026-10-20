package com.proyecto.proyecto_techlab_springboot.dto.image;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ImageReorderRequest(
        @NotEmpty(message = "La lista de identificadores para reordenar no puede estar vacía.")
        List<@NotNull(message = "El ID de imagen no puede ser nulo.") Long> orderedImageIds
) {}