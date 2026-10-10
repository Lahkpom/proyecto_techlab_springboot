package com.proyecto.proyecto_techlab_springboot.dto.variant;

import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.TargetGender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VariantResponse(
        UUID            id,
        UUID            productId,
        String          productName,
        Integer         colorId,
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