package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.dto.product.ProductUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.exception.ProductNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Product;
import com.proyecto.proyecto_techlab_springboot.model.enums.Category;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.repository.iProductRepository;
import com.proyecto.proyecto_techlab_springboot.repository.iVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final iProductRepository iProductRepository;
    private final iVariantRepository iVariantRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .productCategory(Category.CALZADO)
                .productName(request.name())
                .productShortDesc(request.shortDesc())
                .productLongDesc(request.longDesc())
                .build();

        Product saved = iProductRepository.save(product);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return iProductRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        return mapToResponse(this.findProductByIdOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = this.findProductByIdOrThrow(id);

        product.setProductName(request.name());
        product.setProductShortDesc(request.shortDesc());
        product.setProductLongDesc(request.longDesc());

        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(UUID id, StatusUpdateRequest request) {
        Product product = this.findProductByIdOrThrow(id);
        RowStatus lastStatus = product.getRowStatus();
        product.setRowStatus(request.rowStatus());

        if (lastStatus == RowStatus.ACTIVE && request.rowStatus() != RowStatus.ACTIVE)
            iVariantRepository.updateStatusByProductId(id, request.rowStatus(), lastStatus);

        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product entity) {
        return new ProductResponse(
                entity.getProductId(),
                entity.getProductCategory(),
                entity.getProductName(),
                entity.getProductShortDesc(),
                entity.getProductLongDesc(),
                entity.getRowStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Product findProductByIdOrThrow(UUID id) {
        return iProductRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("{ ID: %s }".formatted(id)));
    }
}