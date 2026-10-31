package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaleReturnItemInspectionRequest(
        @NotNull(message = "El identificador del ítem de devolución es obligatorio")
        Long returnItemId,

        @NotNull(message = "Debe dictaminar si la prenda es apta para reingreso a stock (true/false)")
        Boolean restockApproved,

        @Size(max = 255, message = "El motivo de rechazo no puede superar los 255 caracteres")
        String rejectionReason
) {}