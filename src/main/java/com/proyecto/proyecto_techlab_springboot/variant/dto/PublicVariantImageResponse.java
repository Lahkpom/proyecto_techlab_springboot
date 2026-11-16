package com.proyecto.proyecto_techlab_springboot.variant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PublicVariantImageResponse(
        @JsonAlias("image_url")
        String imageUrl,
        @JsonAlias("is_primary")
        boolean isPrimary
) {}
