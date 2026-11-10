package com.proyecto.proyecto_techlab_springboot.variant.dto;

import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.variant.model.TargetGender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VariantResponse(
        Long            id,
        UUID            productId,
        String          productName,
        Long            colorId,
        String          colorName,
        String          colorCode,
        NumericSize     size,
        TargetGender    targetGender,
        String          desc,
        String          sku,
        BigDecimal      price,
        Integer         stock,
        RowStatus       rowStatus,
        Instant         createdAt,
        Instant         updatedAt
) {}