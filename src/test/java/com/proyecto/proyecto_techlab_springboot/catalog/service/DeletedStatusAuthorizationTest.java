package com.proyecto.proyecto_techlab_springboot.catalog.service;

import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.color.repository.IColorRepository;
import com.proyecto.proyecto_techlab_springboot.color.service.ColorService;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.exception.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.variant.repository.IVariantRepository;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DeletedStatusAuthorizationTest {

    private final AuthenticatedUserPrincipal operator =
            new AuthenticatedUserPrincipal(UUID.randomUUID(), "operator@example.com", UserRole.OPERATOR);
    private final StatusUpdateRequest deleteRequest = new StatusUpdateRequest(RowStatus.DELETED);

    @Test
    void operatorCannotDeleteColors() {
        IColorRepository repository = mock(IColorRepository.class);
        ColorService service = new ColorService(repository);

        assertThrows(AccessDeniedException.class, () -> service.updateColorStatus(1L, deleteRequest, operator));
        verifyNoInteractions(repository);
    }

    @Test
    void operatorCannotDeleteProductsOrPublishEvents() {
        IProductRepository repository = mock(IProductRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ProductService service = new ProductService(repository, eventPublisher, new ObjectMapper());

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateProductStatus(UUID.randomUUID(), deleteRequest, operator)
        );
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void operatorCannotDeleteVariants() {
        IVariantRepository repository = mock(IVariantRepository.class);
        VariantService service = new VariantService(
                repository,
                mock(ProductService.class),
                mock(ColorService.class),
                new ObjectMapper()
        );

        assertThrows(AccessDeniedException.class, () -> service.updateVariantStatus(1L, deleteRequest, operator));
        verifyNoInteractions(repository);
    }
}
