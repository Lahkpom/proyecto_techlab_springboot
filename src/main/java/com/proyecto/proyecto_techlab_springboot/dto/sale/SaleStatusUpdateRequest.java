package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload utilizado por los administradores para avanzar la máquina de estados operativa y logísitica de una orden.
 */
public record SaleStatusUpdateRequest(
        @NotNull(message = "El nuevo estado de la venta es obligatorio")
        SaleStatus newStatus,

        @Size(max = 500, message = "El motivo del cambio no puede superar los 500 caracteres")
        String changeReason
) {}