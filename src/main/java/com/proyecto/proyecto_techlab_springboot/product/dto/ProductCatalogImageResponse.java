package com.proyecto.proyecto_techlab_springboot.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ProductCatalogImageResponse(
        @JsonAlias("color_id") Long colorId,
        @JsonAlias("color_name") String colorName,
        @JsonAlias("color_code") String colorCode,
        @JsonAlias("image_url") String imageUrl
) {}
