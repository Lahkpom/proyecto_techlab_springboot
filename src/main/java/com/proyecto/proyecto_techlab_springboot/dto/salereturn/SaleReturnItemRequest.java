package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleReturnItemRequest(
        @NotNull(message = "El identificador del ítem de venta es obligatorio")
        Long saleItemId,

        @NotNull(message = "La cantidad a devolver es obligatoria")
        @Min(value = 1, message = "La cantidad mínima a devolver es 1")
        Integer quantity
) {}