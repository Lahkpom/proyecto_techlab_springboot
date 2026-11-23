package com.proyecto.proyecto_techlab_springboot.image.mapper;

import com.proyecto.proyecto_techlab_springboot.image.dto.ImageResponse;
import com.proyecto.proyecto_techlab_springboot.image.model.Image;
import org.springframework.stereotype.Component;

@Component
public class ImageMapper {

    public ImageResponse toResponse(Image image) {
        return new ImageResponse(
                image.getImageId(),
                image.getProduct().getProductId(),
                image.getColor().getColorId(),
                image.getImageUrl(),
                image.isPrimary(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }
}
