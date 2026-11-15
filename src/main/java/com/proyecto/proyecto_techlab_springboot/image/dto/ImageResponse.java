package com.proyecto.proyecto_techlab_springboot.image.dto;

import java.time.Instant;
import java.util.UUID;

public record ImageResponse(
        Long    imageId,
        UUID    productId,
        Long    colorId,
        String  imageUrl,
        Integer displayOrder,
        boolean isPrimary,
        Instant createdAt,
        Instant updatedAt
) {}