package com.proyecto.proyecto_techlab_springboot.dto.color;

import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import java.time.Instant;

public record ColorResponse(
        Long        id,
        String      name,
        String      desc,
        String      code,
        RowStatus   rowStatus,
        Instant     createdAt,
        Instant     updatedAt
) {}