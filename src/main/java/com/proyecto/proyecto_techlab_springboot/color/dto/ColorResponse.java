package com.proyecto.proyecto_techlab_springboot.color.dto;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
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