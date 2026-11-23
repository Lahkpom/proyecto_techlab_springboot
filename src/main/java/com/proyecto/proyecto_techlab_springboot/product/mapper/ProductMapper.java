package com.proyecto.proyecto_techlab_springboot.product.mapper;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidCatalogDataException;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogImageResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductOptionResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    private static final TypeReference<List<ProductCatalogImageResponse>> CATALOG_IMAGES_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ProductMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProductOptionResponse toOptionResponse(Product product) {
        return new ProductOptionResponse(
                product.getProductId(),
                product.getProductName(),
                product.getTargetGender(),
                product.getRowStatus()
        );
    }

    public ProductResponse toResponse(Product entity) {
        return new ProductResponse(
                entity.getProductId(),
                entity.getProductCategory(),
                entity.getTargetGender(),
                entity.getProductName(),
                entity.getProductShortDesc(),
                entity.getProductLongDesc(),
                entity.getRowStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductCatalogResponse toCatalogResponse(
            IProductRepository.PublicProductCatalogProjection projection
    ) {
        String imagesJson = projection.getMainImagesUrl();
        if (imagesJson == null)
            return toCatalogResponse(projection, List.of());

        List<ProductCatalogImageResponse> images;
        try {
            images = objectMapper.readValue(imagesJson, CATALOG_IMAGES_TYPE);
        } catch (JacksonException ex) {
            throw new InvalidCatalogDataException("No se pudieron interpretar las imágenes de la vista del catálogo.", ex);
        }

        return toCatalogResponse(projection, images);
    }

    private ProductCatalogResponse toCatalogResponse(
            IProductRepository.PublicProductCatalogProjection projection,
            List<ProductCatalogImageResponse> images
    ) {
        return new ProductCatalogResponse(
                projection.getProductId(),
                Category.valueOf(projection.getProductCategory()),
                TargetGender.valueOf(projection.getTargetGender()),
                projection.getProductName(),
                projection.getShortDesc(),
                projection.getMinPrice(),
                projection.getSizeCount(),
                projection.getColorCount(),
                images
        );
    }
}
