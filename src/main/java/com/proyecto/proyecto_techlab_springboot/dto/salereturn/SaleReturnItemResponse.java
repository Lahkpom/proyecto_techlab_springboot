package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.NumericSize;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SaleReturnItemResponse(
        Long        returnItemId,
        Long        saleItemId,
        Long        variantId,
        String      productName,
        String      variantDesc,
        String      variantSku,
        NumericSize variantSize,
        String      colorName,
        String      imageUrl,
        BigDecimal  unitPrice,
        Integer     quantity,
        BigDecimal  subtotal,
        Boolean     restockApproved,
        String      rejectionReason,
        Instant     createdAt
) {}