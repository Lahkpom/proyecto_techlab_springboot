package com.proyecto.proyecto_techlab_springboot.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long        variantId,
        String      productTitle,
        String      variantSku,
        String      attributesDescription,
        String      primaryImageUrl,
        BigDecimal  unitPrice,
        Integer     quantity,
        BigDecimal  subtotal,
        Integer     currentAvailableStock,
        boolean     wasAdjusted
) {}