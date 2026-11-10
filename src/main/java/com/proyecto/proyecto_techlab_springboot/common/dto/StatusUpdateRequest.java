package com.proyecto.proyecto_techlab_springboot.common.dto;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "El estado del registro es obligatorio")
        RowStatus rowStatus
) {}