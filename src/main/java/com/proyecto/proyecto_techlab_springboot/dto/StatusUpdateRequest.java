package com.proyecto.proyecto_techlab_springboot.dto;

import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "El estado del registro es obligatorio")
        RowStatus rowStatus
) {}