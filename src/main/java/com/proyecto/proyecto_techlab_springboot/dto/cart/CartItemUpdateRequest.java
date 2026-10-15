package com.proyecto.proyecto_techlab_springboot.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1      , message = "La cantidad mínima debe ser 1.")
        @Max(value = 10000  , message = "La cantidad no puede superar las 10.000 unidades.")
        Integer quantity
) {}