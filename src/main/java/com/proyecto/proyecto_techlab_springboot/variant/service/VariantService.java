package com.proyecto.proyecto_techlab_springboot.variant.service;

import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.exception.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.color.model.Color;
import com.proyecto.proyecto_techlab_springboot.color.service.ColorService;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductDeletedEvent;
import com.proyecto.proyecto_techlab_springboot.product.event.ProductInactivatedEvent;
import com.proyecto.proyecto_techlab_springboot.product.model.Product;
import com.proyecto.proyecto_techlab_springboot.product.service.ProductService;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.variant.dto.PublicProductVariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantCreateRequest;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.variant.exception.VariantAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.variant.exception.VariantNoLongerAvailableException;
import com.proyecto.proyecto_techlab_springboot.variant.exception.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.variant.model.NumericSize;
import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import com.proyecto.proyecto_techlab_springboot.variant.mapper.VariantMapper;
import com.proyecto.proyecto_techlab_springboot.variant.repository.IVariantRepository;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariantService {

    private final IVariantRepository    iVariantRepository;
    private final ProductService productService;
    private final ColorService colorService;
    private final VariantMapper variantMapper;

    @Transactional(readOnly = true)
    public List<VariantResponse> getVariantsByProduct(UUID productId, boolean includeInactive) {
        this.productService.findProductByIdOrThrow(productId);

        List<Variant> variants = includeInactive
                ? iVariantRepository.findAllByProduct_ProductId(productId)
                : iVariantRepository.findAllByProduct_ProductIdAndRowStatus(productId, RowStatus.ACTIVE);

        return variants.stream().map(this.variantMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<VariantResponse> searchVariantsAdmin(
            UUID productId,
            Long colorId,
            NumericSize size,
            RowStatus rowStatus,
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = search == null || search.isBlank()
                ? "%%"
                : "%%%s%%".formatted(search.trim());
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return iVariantRepository.searchVariantsAdmin(
                productId != null,
                productId,
                colorId != null,
                colorId,
                size != null,
                size,
                rowStatus != null,
                rowStatus,
                normalizedSearch,
                RowStatus.ACTIVE,
                unsortedPageable
        ).map(this.variantMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PublicProductVariantResponse> getPublicVariantsByProduct(UUID productId) {
        productService.getPublicProductById(productId);
        return iVariantRepository.findPublicVariantsByProductId(productId).stream()
                .map(this.variantMapper::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse getVariantById(Long id) {
        return this.variantMapper.toResponse(findVariantByIdOrThrow(id));
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
                .variantDesc(request.desc().trim())
                .variantSku(newSku)
                .variantPrice(request.price())
                .variantStock(request.stock())
                .rowStatus(RowStatus.ACTIVE)
                .build();

        Variant saved = iVariantRepository.save(variant);

        log.info("Variante creada exitosamente { variantId: {}, productId: {}, SKU: {} }", saved.getVariantId(), saved.getProduct().getProductId(), saved.getVariantSku());

        return this.variantMapper.toResponse(saved);
    }

    @Transactional
    public VariantResponse updateVariant(Long id, VariantUpdateRequest request) {
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
        variant.setVariantDesc(request.desc().trim());
        variant.setVariantSku(newSku);
        variant.setVariantPrice(request.price());
        variant.setVariantStock(request.stock());

        log.info("Variante modificada exitosamente { variantId: {}, productId: {}, SKU: {} }", variant.getVariantId(), variant.getProduct().getProductId(), variant.getVariantSku());

        return this.variantMapper.toResponse(variant);
    }

    @Transactional
    public VariantResponse updateVariantStatus(
            Long id,
            StatusUpdateRequest request,
            AuthenticatedUserPrincipal actor
    ) {
        if (request.rowStatus() == RowStatus.DELETED
                && actor.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede eliminar registros de forma lógica.");
        }

        Variant     variant     = findVariantByIdOrThrow(id);
        RowStatus   lastStatus  = variant.getRowStatus();

        variant.setRowStatus(request.rowStatus());

        log.info("Estado de la variante modificado exitosamente { ID: {}, Estado Anterior: {}, Nuevo Estado: {} }", variant.getVariantId(), lastStatus, variant.getRowStatus());

        return this.variantMapper.toResponse(variant);
    }

    /**
     * Restituye el stock físico de un conjunto de ítems (usado en cancelaciones y devoluciones).
     * Bloquea las variantes con SELECT FOR UPDATE para evitar condiciones de carrera.
     */
    @Transactional
    public void restoreStock(Map<Long, Integer> itemsToRestore) {
        if (itemsToRestore == null || itemsToRestore.isEmpty()) return;

        List<Long>      variantIds      = itemsToRestore.keySet().stream().toList();
        List<Variant>   lockedVariants  = this.findAllByIdInWithLock(variantIds);

        for (Variant variant : lockedVariants) {
            Integer quantity = itemsToRestore.get(variant.getVariantId());
            
            if (quantity != null && quantity > 0)
                variant.setVariantStock(variant.getVariantStock() + quantity);
        }
    }

    @Transactional
    public void restoreStockForSaleItems(Collection<SaleItem> saleItems) {
        Map<Long, Integer> quantitiesByVariant = saleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(
                        item -> item.getVariant().getVariantId(),
                        SaleItem::getQuantity,
                        Integer::sum
                ));
        restoreStock(quantitiesByVariant);
    }

    @EventListener
    @Transactional
    public void onProductInactivated(ProductInactivatedEvent event) {
        this.iVariantRepository.inactivateVariantsByProductId(event.productId());
    }

    @EventListener
    @Transactional
    public void onProductDeleted(ProductDeletedEvent event) {
        this.iVariantRepository.deleteVariantsByProductId(event.productId());
    }

    public Variant findVariantByIdOrThrow(Long id) {
        return iVariantRepository.findById(id)
                .orElseThrow(() -> new VariantNotFoundException("{ ID: %s }".formatted(id)));
    }

    public List<Variant> findAllByIdInWithLock(List<Long> variantIds){
        return this.iVariantRepository.findAllByIdInWithLock(variantIds);
    }

    @Transactional(readOnly = true)
    public List<Variant> findAllByIdInWithDetails(Collection<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty())
            return List.of();

        return this.iVariantRepository.findAllByIdInWithDetails(variantIds);
    }

    public void requireAvailableVariantOrThrow(Variant variant){
        if (variant.getRowStatus() != RowStatus.ACTIVE)
            throw new VariantNoLongerAvailableException("{ ID: %s }".formatted(variant.getVariantId()));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveVariant(UUID productId, Long colorId) {
        return this.iVariantRepository.countActiveByProductIdAndColorId(
                productId,
                colorId,
                RowStatus.ACTIVE
        ) > 0;
    }

}