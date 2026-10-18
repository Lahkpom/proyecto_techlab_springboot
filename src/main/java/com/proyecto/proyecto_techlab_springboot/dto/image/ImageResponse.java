package com.proyecto.proyecto_techlab_springboot.dto.image;


import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;

public record ImageResponse(
        Long        imageId,
        Long        variantId,
        String      imageUrl,
        Integer     displayOrder,
        boolean     isPrimary,
        RowStatus   rowStatus
) {}