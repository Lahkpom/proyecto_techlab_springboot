package com.proyecto.proyecto_techlab_springboot.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID                    userId,
        List<CartItemResponse>  items,
        Integer                 totalUnits,
        BigDecimal              grandTotal,
        boolean                 itemsPurged,
        boolean                 itemsAdjusted
) {}