package com.proyecto.proyecto_techlab_springboot.product.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogImageResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCatalogResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductCreateRequest;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductResponse;
import com.proyecto.proyecto_techlab_springboot.product.dto.ProductUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductInactivatedEvent;
import com.proyecto.proyecto_techlab_springboot.product.exception.InvalidProductCatalogSortException;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.product.exception.ProductNotFoundException;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductDeletedEvent;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.model.Category;
import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.product.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final TypeReference<List<ProductCatalogImageResponse>> CATALOG_IMAGES_TYPE = new TypeReference<>() {};

    private static final Map<String, String> CATALOG_SORT_COLUMNS = Map.of(
            "id", "product_id",
            "category", "product_category",
            "targetGender", "target_gender",
            "name", "product_name",
            "shortDesc", "short_desc",
            "minPrice", "min_price",
            "sizeCount", "size_count",
            "colorCount", "color_count"
    );

    private final IProductRepository    iProductRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

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
    public Page<ProductCatalogResponse> getAllPublicProducts(TargetGender targetGender, Pageable pageable) {
        Pageable catalogPageable = toCatalogPageable(pageable);
        return this.iProductRepository.findPublicCatalog(targetGender.name(), catalogPageable)
                .map(this::mapToCatalogResponse);
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
                this.eventPublisher.publishEvent(new ProductInactivatedEvent(id));

            if (request.rowStatus() == RowStatus.DELETED)
                this.eventPublisher.publishEvent(new ProductDeletedEvent(id));
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

    private ProductCatalogResponse mapToCatalogResponse(
            IProductRepository.PublicProductCatalogProjection projection
    ) {
        String imagesJson = projection.getMainImagesUrl();
        if (imagesJson == null)
            return mapToCatalogResponse(projection, List.of());

        List<ProductCatalogImageResponse> images;
        try {
            images = objectMapper.readValue(imagesJson, CATALOG_IMAGES_TYPE);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No se pudo interpretar las imágenes de la view del catálogo.", ex);
        }

        return mapToCatalogResponse(projection, images);
    }

    private ProductCatalogResponse mapToCatalogResponse(
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

    private Pageable toCatalogPageable(Pageable pageable) {
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by("product_name").ascending().and(Sort.by("product_id").ascending())
                : Sort.by(
                    pageable.getSort().stream()
                            .map(order -> {
                                String column = CATALOG_SORT_COLUMNS.get(order.getProperty());
                                if (column == null)
                                    throw new InvalidProductCatalogSortException(order.getProperty());
                                return order.withProperty(column);
                            })
                            .toList()
                );

        if (sort.getOrderFor("product_id") == null)
            sort = sort.and(Sort.by("product_id").ascending());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    public Product findProductByIdOrThrow(UUID id) {
        return iProductRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("{ ID: %s }".formatted(id)));
    }
}