package com.proyecto.proyecto_techlab_springboot.color.mapper;

import com.proyecto.proyecto_techlab_springboot.color.dto.ColorResponse;
import com.proyecto.proyecto_techlab_springboot.color.model.Color;
import org.springframework.stereotype.Component;

@Component
public class ColorMapper {

    public ColorResponse toResponse(Color color) {
        return new ColorResponse(
                color.getColorId(),
                color.getColorName(),
                color.getColorDesc(),
                color.getColorCode(),
                color.getRowStatus(),
                color.getCreatedAt(),
                color.getUpdatedAt()
        );
    }
}
