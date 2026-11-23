package com.proyecto.proyecto_techlab_springboot.variant.mapper;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidCatalogDataException;
import com.proyecto.proyecto_techlab_springboot.variant.dto.PublicProductVariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.dto.PublicVariantImageResponse;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;
import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import com.proyecto.proyecto_techlab_springboot.variant.repository.IVariantRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VariantMapper {

    private static final TypeReference<List<PublicVariantImageResponse>> PUBLIC_IMAGES_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public VariantMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VariantResponse toResponse(Variant v) {
        return new VariantResponse(
                v.getVariantId(),
                v.getProduct().getProductId(),
                v.getProduct().getProductName(),
                v.getColor().getColorId(),
                v.getColor().getColorName(),
                v.getColor().getColorCode(),
                v.getVariantSize(),
                v.getVariantDesc(),
                v.getVariantSku(),
                v.getVariantPrice(),
                v.getVariantStock(),
                v.getRowStatus(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }

    public PublicProductVariantResponse toPublicResponse(
            IVariantRepository.PublicProductVariantProjection projection
    ) {
        try {
            String imagesJson = projection.getImagesJson();
            List<PublicVariantImageResponse> images = imagesJson == null || imagesJson.isBlank()
                    ? List.of()
                    : objectMapper.readValue(imagesJson, PUBLIC_IMAGES_TYPE);

            if (images == null) images = List.of();

            return new PublicProductVariantResponse(
                    projection.getProductId(),
                    projection.getProductName(),
                    projection.getProductLongDesc(),
                    projection.getVariantId(),
                    NumericSize.valueOf(projection.getVariantSize()),
                    projection.getPrice(),
                    projection.getStock(),
                    projection.getColorId(),
                    projection.getColorName(),
                    projection.getColorCode(),
                    images
            );
        } catch (JacksonException | IllegalArgumentException ex) {
            throw new InvalidCatalogDataException(
                    "La vista pública de variantes contiene datos inválidos.",
                    ex
            );
        }
    }
}
