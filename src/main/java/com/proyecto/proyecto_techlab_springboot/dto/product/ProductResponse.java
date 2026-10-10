package com.proyecto.proyecto_techlab_springboot.dto.product;

import com.proyecto.proyecto_techlab_springboot.model.enums.Category;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;

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