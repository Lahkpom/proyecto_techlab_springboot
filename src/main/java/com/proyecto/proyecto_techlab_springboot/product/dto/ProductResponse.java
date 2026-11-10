package com.proyecto.proyecto_techlab_springboot.product.dto;

import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID        id,
        Category    category,
        String      name,
        String      shortDesc,
        String      longDesc,
        RowStatus   status,
        Instant     createdAt,
        Instant     updatedAt
) {}