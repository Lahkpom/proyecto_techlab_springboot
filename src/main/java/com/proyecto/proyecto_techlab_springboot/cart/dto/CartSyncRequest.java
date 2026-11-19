package com.proyecto.proyecto_techlab_springboot.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartSyncRequest(
        @NotNull @Valid List<CartItemRequest> items
) {}
