package com.proyecto.proyecto_techlab_springboot.dto.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
        @NotNull(message = "El variantId es obligatorio")
        Long variantId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima por ítem es 1")
        Integer quantity
) {}