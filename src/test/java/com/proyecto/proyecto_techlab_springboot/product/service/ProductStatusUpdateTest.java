package com.proyecto.proyecto_techlab_springboot.product.service;

import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductDeletedEvent;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductInactivatedEvent;
import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
import com.proyecto.proyecto_techlab_springboot.product.mapper.ProductMapper;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ProductStatusUpdateTest {

    private final IProductRepository repository = mock(IProductRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ProductService service = new ProductService(
            repository,
            eventPublisher,
            new ProductMapper(new ObjectMapper())
    );
    private final AuthenticatedUserPrincipal admin =
            new AuthenticatedUserPrincipal(UUID.randomUUID(), "admin@example.com", UserRole.ADMIN);

    @Test
    void inactivationIsFlushedBeforePublishingItsEvent() {
        assertStatusIsFlushedBeforeEvent(RowStatus.INACTIVE, new ProductInactivatedEvent(productId));
    }

    @Test
    void deletionIsFlushedBeforePublishingItsEvent() {
        assertStatusIsFlushedBeforeEvent(RowStatus.DELETED, new ProductDeletedEvent(productId));
    }

    private final UUID productId = UUID.randomUUID();

    private void assertStatusIsFlushedBeforeEvent(RowStatus status, Object event) {
        Product product = spy(Product.builder()
                .productId(productId)
                .productCategory(Category.CALZADO)
                .targetGender(TargetGender.UNISEX)
                .productName("Producto")
                .productShortDesc("Descripción corta")
                .productLongDesc("Descripción")
                .rowStatus(RowStatus.ACTIVE)
                .build());
        when(repository.findById(productId)).thenReturn(Optional.of(product));
        when(repository.saveAndFlush(product)).thenReturn(product);

        var response = service.updateProductStatus(productId, new StatusUpdateRequest(status), admin);

        var order = inOrder(product, repository, eventPublisher);
        order.verify(product).setRowStatus(status);
        order.verify(repository).saveAndFlush(product);
        order.verify(eventPublisher).publishEvent(event);
        assertEquals(status, response.status());
    }
}
