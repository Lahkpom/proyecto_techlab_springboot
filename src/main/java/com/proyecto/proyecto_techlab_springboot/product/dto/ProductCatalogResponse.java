package com.proyecto.proyecto_techlab_springboot.product.dto;

import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductCatalogResponse(
        UUID id,
        Category category,
        TargetGender targetGender,
        String name,
        String shortDesc,
        BigDecimal minPrice,
        Long sizeCount,
        Long colorCount,
        List<ProductCatalogImageResponse> mainImagesUrl
) {}
