package com.proyecto.proyecto_techlab_springboot.product.service;

import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogResponse;
import com.proyecto.proyecto_techlab_springboot.product.exception.InvalidProductCatalogSortException;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCatalogServiceTest {

    @Test
    void returnsCatalogFieldsAndFiltersByTargetGender() {
        IProductRepository repository = mock(IProductRepository.class);
        IProductRepository.PublicProductCatalogProjection projection =
                mock(IProductRepository.PublicProductCatalogProjection.class);
        UUID productId = UUID.randomUUID();

        when(projection.getProductId()).thenReturn(productId);
        when(projection.getProductCategory()).thenReturn("CALZADO");
        when(projection.getTargetGender()).thenReturn("MUJERES");
        when(projection.getProductName()).thenReturn("Bota");
        when(projection.getShortDesc()).thenReturn("Cuero");
        when(projection.getMinPrice()).thenReturn(new BigDecimal("12500.00"));
        when(projection.getSizeCount()).thenReturn(3L);
        when(projection.getColorCount()).thenReturn(2L);
        when(projection.getMainImagesUrl()).thenReturn(
                "[{\"color_id\":7,\"color_name\":\"Negro\",\"color_code\":\"#000000\",\"image_url\":\"https://example.test/boot.jpg\"}]"
        );
        when(repository.findPublicCatalog(eq("MUJERES"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        ProductService service = new ProductService(
                repository,
                mock(VariantService.class),
                mock(ApplicationEventPublisher.class),
                new ObjectMapper()
        );

        ProductCatalogResponse result = service.getAllPublicProducts(
                TargetGender.MUJERES,
                PageRequest.of(0, 20)
        ).getContent().getFirst();

        assertEquals(productId, result.id());
        assertEquals(TargetGender.MUJERES, result.targetGender());
        assertEquals(new BigDecimal("12500.00"), result.minPrice());
        assertEquals(3L, result.sizeCount());
        assertEquals(7L, result.mainImagesUrl().getFirst().colorId());
        assertEquals("https://example.test/boot.jpg", result.mainImagesUrl().getFirst().imageUrl());
        verify(repository).findPublicCatalog(eq("MUJERES"), argThat(pageable ->
                pageable.getSort().getOrderFor("product_name") != null
                        && pageable.getSort().getOrderFor("product_id") != null
        ));
    }

    @Test
    void returnsProductsWithoutImagesWithAnEmptyImageList() {
        IProductRepository repository = mock(IProductRepository.class);
        IProductRepository.PublicProductCatalogProjection projection =
                mock(IProductRepository.PublicProductCatalogProjection.class);

        when(projection.getProductId()).thenReturn(UUID.randomUUID());
        when(projection.getProductCategory()).thenReturn("CALZADO");
        when(projection.getTargetGender()).thenReturn("MUJERES");
        when(projection.getProductName()).thenReturn("Bota");
        when(projection.getShortDesc()).thenReturn("Cuero");
        when(projection.getMinPrice()).thenReturn(new BigDecimal("12500.00"));
        when(projection.getSizeCount()).thenReturn(3L);
        when(projection.getColorCount()).thenReturn(2L);
        when(projection.getMainImagesUrl()).thenReturn(null);
        when(repository.findPublicCatalog(eq("MUJERES"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        ProductService service = new ProductService(
                repository,
                mock(VariantService.class),
                mock(ApplicationEventPublisher.class),
                new ObjectMapper()
        );

        ProductCatalogResponse result = service.getAllPublicProducts(
                TargetGender.MUJERES,
                PageRequest.of(0, 20)
        ).getContent().getFirst();

        assertEquals(List.of(), result.mainImagesUrl());
    }

    @Test
    void rejectsUnsupportedCatalogSortFields() {
        IProductRepository repository = mock(IProductRepository.class);
        ProductService service = new ProductService(
                repository,
                mock(VariantService.class),
                mock(ApplicationEventPublisher.class),
                new ObjectMapper()
        );

        assertThrows(InvalidProductCatalogSortException.class, () ->
                service.getAllPublicProducts(
                        TargetGender.MUJERES,
                        PageRequest.of(0, 20, Sort.by("createdAt"))
                )
        );
        verify(repository, never()).findPublicCatalog(any(), any(Pageable.class));
    }
}
