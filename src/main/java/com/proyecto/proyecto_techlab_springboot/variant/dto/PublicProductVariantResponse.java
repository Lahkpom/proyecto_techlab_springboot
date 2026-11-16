package com.proyecto.proyecto_techlab_springboot.variant.dto;

import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicProductVariantResponse(
        UUID productId,
        String productName,
        String productLongDesc,
        Long variantId,
        NumericSize size,
        BigDecimal price,
        Integer stock,
        Long colorId,
        String colorName,
        String colorCode,
        List<PublicVariantImageResponse> images
) {}
