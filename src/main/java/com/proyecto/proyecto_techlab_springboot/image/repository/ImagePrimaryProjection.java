package com.proyecto.proyecto_techlab_springboot.image.repository;

import java.util.UUID;

public interface ImagePrimaryProjection {
    UUID getProductId();
    Long getColorId();
    String getImageUrl();
}
