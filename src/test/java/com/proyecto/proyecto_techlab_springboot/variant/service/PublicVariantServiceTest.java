package com.proyecto.proyecto_techlab_springboot.variant.service;

import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.color.service.ColorService;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import com.proyecto.proyecto_techlab_springboot.variant.dto.PublicProductVariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;
import com.proyecto.proyecto_techlab_springboot.variant.repository.IVariantRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicVariantServiceTest {

    @Test
    void returnsPublicVariantFieldsAndImages() {
        IVariantRepository repository = mock(IVariantRepository.class);
        ProductService productService = mock(ProductService.class);
        UUID productId = UUID.randomUUID();
        IVariantRepository.PublicProductVariantProjection projection =
                mock(IVariantRepository.PublicProductVariantProjection.class);

        when(projection.getProductId()).thenReturn(productId);
        when(projection.getProductName()).thenReturn("Bota");
        when(projection.getProductLongDesc()).thenReturn("Bota de cuero");
        when(projection.getVariantId()).thenReturn(27L);
        when(projection.getVariantSize()).thenReturn("T_38");
        when(projection.getPrice()).thenReturn(new BigDecimal("12500.00"));
        when(projection.getStock()).thenReturn(4);
        when(projection.getColorId()).thenReturn(7L);
        when(projection.getColorName()).thenReturn("Negro");
        when(projection.getColorCode()).thenReturn("#000000");
        when(projection.getImagesJson()).thenReturn(
                "[{\"image_url\":\"https://example.test/boot.jpg\",\"is_primary\":true}]"
        );
        when(repository.findPublicVariantsByProductId(productId)).thenReturn(List.of(projection));

        VariantService service = new VariantService(
                repository,
                productService,
                mock(ColorService.class),
                new ObjectMapper()
        );

        PublicProductVariantResponse result = service.getPublicVariantsByProduct(productId).getFirst();

        assertEquals(productId, result.productId());
        assertEquals("Bota de cuero", result.productLongDesc());
        assertEquals(27L, result.variantId());
        assertEquals(NumericSize.T_38, result.size());
        assertEquals(new BigDecimal("12500.00"), result.price());
        assertEquals(4, result.stock());
        assertEquals(7L, result.colorId());
        assertEquals("Negro", result.colorName());
        assertEquals("#000000", result.colorCode());
        assertEquals("https://example.test/boot.jpg", result.images().getFirst().imageUrl());
        assertTrue(result.images().getFirst().isPrimary());
        verify(productService).getPublicProductById(productId);
    }

    @Test
    void returnsAnEmptyListForAnAvailableProductWithoutActiveVariants() {
        IVariantRepository repository = mock(IVariantRepository.class);
        ProductService productService = mock(ProductService.class);
        UUID productId = UUID.randomUUID();
        when(repository.findPublicVariantsByProductId(productId)).thenReturn(List.of());

        VariantService service = new VariantService(
                repository,
                productService,
                mock(ColorService.class),
                new ObjectMapper()
        );

        assertEquals(List.of(), service.getPublicVariantsByProduct(productId));
        verify(productService).getPublicProductById(productId);
        verify(repository).findPublicVariantsByProductId(productId);
    }

    @Test
    void returnsAnEmptyImageListWhenTheVariantHasNoImages() {
        IVariantRepository repository = mock(IVariantRepository.class);
        ProductService productService = mock(ProductService.class);
        UUID productId = UUID.randomUUID();
        IVariantRepository.PublicProductVariantProjection projection =
                mock(IVariantRepository.PublicProductVariantProjection.class);

        when(projection.getProductId()).thenReturn(productId);
        when(projection.getProductName()).thenReturn("Bota");
        when(projection.getProductLongDesc()).thenReturn("Bota de cuero");
        when(projection.getVariantId()).thenReturn(27L);
        when(projection.getVariantSize()).thenReturn("T_38");
        when(projection.getPrice()).thenReturn(new BigDecimal("12500.00"));
        when(projection.getStock()).thenReturn(0);
        when(projection.getColorId()).thenReturn(7L);
        when(projection.getColorName()).thenReturn("Negro");
        when(projection.getColorCode()).thenReturn("#000000");
        when(projection.getImagesJson()).thenReturn(null);
        when(repository.findPublicVariantsByProductId(productId)).thenReturn(List.of(projection));

        VariantService service = new VariantService(
                repository,
                productService,
                mock(ColorService.class),
                new ObjectMapper()
        );

        PublicProductVariantResponse result = service.getPublicVariantsByProduct(productId).getFirst();

        assertEquals(List.of(), result.images());
    }

    @Test
    void doesNotQueryVariantsWhenTheProductIsUnavailable() {
        IVariantRepository repository = mock(IVariantRepository.class);
        ProductService productService = mock(ProductService.class);
        UUID productId = UUID.randomUUID();
        when(productService.getPublicProductById(productId))
                .thenThrow(new ProductNoLongerAvailableException(
                        "{ ID: %s }".formatted(productId)
                ));

        VariantService service = new VariantService(
                repository,
                productService,
                mock(ColorService.class),
                new ObjectMapper()
        );

        assertThrows(
                ProductNoLongerAvailableException.class,
                () -> service.getPublicVariantsByProduct(productId)
        );
        verify(repository, never()).findPublicVariantsByProductId(productId);
    }
}
