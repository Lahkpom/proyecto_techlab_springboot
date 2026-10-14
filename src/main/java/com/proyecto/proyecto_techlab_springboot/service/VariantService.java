package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.common.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantResponse;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Color;
import com.proyecto.proyecto_techlab_springboot.model.Product;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.enums.RowStatus;
import com.proyecto.proyecto_techlab_springboot.repository.IVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariantService {

    private final IVariantRepository    iVariantRepository;
    private final ProductService        productService;
    private final ColorService          colorService;

    @Transactional(readOnly = true)
    public List<VariantResponse> getVariantsByProduct(UUID productId, boolean includeInactive) {
        this.productService.findProductByIdOrThrow(productId);

        List<Variant> variants = includeInactive
                ? iVariantRepository.findAllByProduct_ProductId(productId)
                : iVariantRepository.findAllByProduct_ProductIdAndRowStatus(productId, RowStatus.ACTIVE);

        return variants.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse getVariantById(UUID id) {
        return mapToResponse(findVariantByIdOrThrow(id));
    }

    @Transactional
    public VariantResponse createVariant(VariantCreateRequest request) {
        String newSku = request.sku().toUpperCase().trim();

        // Chequeo de SKU único
        if (iVariantRepository.existsByVariantSkuIgnoreCase(newSku))
            throw new VariantAlreadyExistsException("{ SKU: %s }".formatted(newSku));

        // Chequeo de combinación única (producto + color + talle)
        if (iVariantRepository.existsByProduct_ProductIdAndColor_ColorIdAndVariantSize(
                request.productId(),
                request.colorId(),
                request.size())
        ) throw new VariantAlreadyExistsException("{ product_id: %s,  color_id: %s, size: %s }".formatted(request.productId(), request.colorId(), request.size()));

        // Referencias
        Product product = this.productService.findProductByIdOrThrow(request.productId());

        Color color = this.colorService.findColorByIdOrThrow(request.colorId());

        Variant variant = Variant.builder()
                .product(product)
                .color(color)
                .variantSize(request.size())
                .targetGender(request.targetGender())
                .variantDesc(request.desc().trim())
                .variantSku(newSku)
                .variantPrice(request.price())
                .variantStock(request.stock())
                .rowStatus(RowStatus.ACTIVE)
                .build();

        Variant saved = iVariantRepository.save(variant);

        log.info("Variante creada exitosamente { variantId: {}, productId: {}, SKU: {} }", saved.getVariantId(), saved.getProduct().getProductId(), saved.getVariantSku());

        return mapToResponse(saved);
    }

    @Transactional
    public VariantResponse updateVariant(UUID id, VariantUpdateRequest request) {
        Variant variant = findVariantByIdOrThrow(id);
        String  newSku  = request.sku().toUpperCase().trim();

        // Si cambió el SKU, validar que no exista en otra variante
        if (iVariantRepository.existsByVariantSkuIgnoreCaseAndVariantIdNot(newSku, id))
            throw new VariantAlreadyExistsException("{ SKU: %s }".formatted(newSku));

        // Si cambió el color o el talle, validar la combinación compuesta
        boolean colorHasChanged = !variant.getColor().getColorId().equals(request.colorId());
        boolean sizeHasChanged  = !variant.getVariantSize().equals(request.size());

        if (colorHasChanged || sizeHasChanged) {
            if (colorHasChanged) variant.setColor(this.colorService.findColorByIdOrThrow(request.colorId()));

            if (iVariantRepository.existsByProduct_ProductIdAndColor_ColorIdAndVariantSizeAndVariantIdNot(
                    variant.getProduct().getProductId(),
                    request.colorId(),
                    request.size(),
                    id
            )) {
                throw new VariantAlreadyExistsException(
                        "{ product_id: %s, color_id: %s, size: %s }".formatted(
                                variant.getProduct().getProductId(),
                                request.colorId(),
                                request.size()
                        )
                );
            }
        }

        variant.setVariantSize(request.size());
        variant.setTargetGender(request.targetGender());
        variant.setVariantDesc(request.desc().trim());
        variant.setVariantSku(newSku);
        variant.setVariantPrice(request.price());
        variant.setVariantStock(request.stock());

        log.info("Variante modificada exitosamente { variantId: {}, productId: {}, SKU: {} }", variant.getVariantId(), variant.getProduct().getProductId(), variant.getVariantSku());

        return mapToResponse(variant);
    }

    @Transactional
    public VariantResponse updateVariantStatus(UUID id, StatusUpdateRequest request) {
        Variant variant = findVariantByIdOrThrow(id);
        RowStatus lastStatus = variant.getRowStatus();
        variant.setRowStatus(request.rowStatus());
        log.info("Estado de la variante modificado exitosamente { ID: {}, Estado Anterior: {}, Nuevo Estado: {} }", variant.getVariantId(), lastStatus, variant.getRowStatus());
        return mapToResponse(variant);
    }

    private Variant findVariantByIdOrThrow(UUID id) {
        return iVariantRepository.findById(id)
                .orElseThrow(() -> new VariantNotFoundException("{ ID: %s }".formatted(id)));
    }

    private VariantResponse mapToResponse(Variant v) {
        return new VariantResponse(
                v.getVariantId(),
                v.getProduct().getProductId(),
                v.getProduct().getProductName(),
                v.getColor().getColorId(),
                v.getColor().getColorName(),
                v.getColor().getColorCode(),
                v.getVariantSize(),
                v.getTargetGender(),
                v.getVariantDesc(),
                v.getVariantSku(),
                v.getVariantPrice(),
                v.getVariantStock(),
                v.getRowStatus(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}