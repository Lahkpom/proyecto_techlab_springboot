package com.proyecto.proyecto_techlab_springboot.product.service;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCreateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductNotFoundException;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductDeletedEvent;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final IProductRepository    iProductRepository;
    private final VariantService        variantService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String productName = request.name().trim();
        if (iProductRepository.existsByProductNameIgnoreCase(productName))
            throw new ProductAlreadyExistsException("{ name: %s }".formatted(productName));

        Product product = Product.builder()
                .productCategory(Category.CALZADO)
                .targetGender(request.targetGender())
                .productName(productName)
                .productShortDesc(request.shortDesc())
                .productLongDesc(request.longDesc())
                .build();

        Product saved = this.iProductRepository.save(product);

        log.info("Producto creado exitosamente { ID: {}, categoría: {}, nombre: {} }", saved.getProductId(), saved.getProductCategory(), saved.getProductName());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllPublicProducts(Pageable pageable) {
        return this.iProductRepository.findAllPublic(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllAdminProducts(Pageable pageable) {
        return this.iProductRepository.findAllForAdmin(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProductById(UUID id) {
        Product p = this.findProductByIdOrThrow(id);

        if (p.getRowStatus() != RowStatus.ACTIVE) 
            throw new ProductNoLongerAvailableException("{ ID: %s }".formatted(id));

        return mapToResponse(p);
    }

    @Transactional(readOnly = true)
    public ProductResponse getAdminProductById(UUID id) {
        return mapToResponse(this.findProductByIdOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = this.findProductByIdOrThrow(id);

        String productName = request.name().trim();
        if (iProductRepository.existsByProductNameIgnoreCaseAndProductIdNot(productName, id))
            throw new ProductAlreadyExistsException("{ name: %s }".formatted(productName));

        product.setProductName(productName);
        product.setTargetGender(request.targetGender());
        product.setProductShortDesc(request.shortDesc());
        product.setProductLongDesc(request.longDesc());

        log.info("Producto modificado exitosamente { ID: {} }", product.getProductId());

        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(UUID id, StatusUpdateRequest request) {
        Product product = this.findProductByIdOrThrow(id);
        RowStatus lastStatus = product.getRowStatus();

        if (lastStatus != request.rowStatus()) {
            if (request.rowStatus() == RowStatus.INACTIVE && lastStatus == RowStatus.ACTIVE)
                this.variantService.inactivateVariantsByProductId(id);

            if (request.rowStatus() == RowStatus.DELETED) {
                this.variantService.deleteVariantsByProductId(id);
                this.eventPublisher.publishEvent(new ProductDeletedEvent(id));
            }
        }
        product.setRowStatus(request.rowStatus());

        log.info("Estado del Producto modificado exitosamente { ID: {}, Estado Anterior: {}, Nuevo Estado: {} }", product.getProductId(), lastStatus, product.getRowStatus());

        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product entity) {
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

    public Product findProductByIdOrThrow(UUID id) {
        return iProductRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("{ ID: %s }".formatted(id)));
    }
}