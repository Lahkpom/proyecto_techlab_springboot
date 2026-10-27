package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detalle inmutable de cada ítem tal como quedó congelado en la compra.
 */
@Builder
public record SaleItemResponse(
        Long        saleItemId,
        Long        variantId,
        String      productName,
        String      productDesc,
        String      variantDesc,
        String      variantSku,
        NumericSize variantSize,
        String      colorName,
        String      imageUrl,
        BigDecimal  unitPrice,
        Integer     quantity,
        BigDecimal  subtotal,
        Instant     createdAt
) {}