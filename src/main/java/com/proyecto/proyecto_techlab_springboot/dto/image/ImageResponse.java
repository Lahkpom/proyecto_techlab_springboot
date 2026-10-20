package com.proyecto.proyecto_techlab_springboot.dto.image;

import java.time.Instant;

public record ImageResponse(
        Long    imageId,
        Long    variantId,
        String  imageUrl,
        Integer displayOrder,
        boolean isPrimary,
        Instant createdAt,
        Instant updatedAt
) {}