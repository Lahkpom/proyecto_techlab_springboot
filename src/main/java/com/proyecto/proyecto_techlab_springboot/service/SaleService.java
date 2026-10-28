package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.sale.*;
import com.proyecto.proyecto_techlab_springboot.exception.common.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.EmptySaleItemsException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.*;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.service.policy.SaleCalculationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Servicio que atiende al cliente final:
 *  - Maneja compras de usuarios registrados (aprovechando su carrito o items enviados) e invitados.
 *  - Aplica bloqueo pesimista sobre las variantes para evitar sobreventas.
 *  - Valida y descuenta stock atómicamente.
 *  - Congela el snapshot de cada variante (incluyendo su imagen de portada).
 *  - Invoca a SalePolicyService para calcular totales y descuentos.
 *  - Persiste la orden y expone las consultas por trackingToken (Magic Link) y por usuario logueado.
 */
@Service
@RequiredArgsConstructor
public class SaleService {

    private final ISaleRepository iSaleRepository;

    private final SalePolicyService salePolicyService;
    private final VariantService    variantService;
    private final ImageService      imageService;
    private final UserService       userService;

    /**
     * Orquestación transaccional unificada para checkout (registrado o invitado).
     */
    @Transactional
    public SaleResponse createSale(CheckoutRequest request, UUID authenticatedUserId) {
        if (request.items() == null || request.items().isEmpty())
            throw new EmptySaleItemsException();

        // Resolver usuario y rol del comprador
        Users       buyer       = null;
        UserRole    buyerRole   = null;
        if (authenticatedUserId != null) {
            buyer       = this.userService.findUserByIdOrThrow(authenticatedUserId);
            buyerRole   = buyer.getUserRole();
        }

        // Extraer IDs de variantes y bloquearlas de forma pesimista en la DB
        List<Long> variantIds = request.items().stream()
                .map(CheckoutItemRequest::variantId)
                .distinct()
                .toList();

        List<Variant>       lockedVariants  = this.variantService.findAllByIdInWithLock(variantIds);
        Map<Long, Variant>  variantMap      = new HashMap<>();
        for (Variant v : lockedVariants) {
            variantMap.put(v.getVariantId(), v);
        }

        // Validar stock, estado y descontar unidades
        int             totalQuantity       = 0;
        BigDecimal      calculatedSubtotal  = BigDecimal.ZERO;
        List<SaleItem>  saleItems           = new ArrayList<>();

        for (CheckoutItemRequest itemReq : request.items()) {
            Variant variant = variantMap.get(itemReq.variantId());

            if (variant == null) throw new VariantNotFoundException("{ ID: %s }".formatted(itemReq.variantId()));

            this.variantService.requireAvailableVariantOrThrow(variant);

            if (variant.getVariantStock() < itemReq.quantity())
                throw new InsufficientStockException(
                        "{ ID: %s, SKU: %s, Disponible: %d, Solicitado: %d }"
                                .formatted(
                                    variant.getVariantId(),
                                    variant.getVariantSku(),
                                    variant.getVariantStock(),
                                    itemReq.quantity()
                                )
                );

            // Descuento atómico de stock en memoria (sincronizado con la fila bloqueada)
            variant.setVariantStock(variant.getVariantStock() - itemReq.quantity());

            BigDecimal lineSubtotal = variant.getVariantPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            calculatedSubtotal      = calculatedSubtotal.add(lineSubtotal);
            totalQuantity           += itemReq.quantity();

            // Buscar la imagen principal congelada
            String mainImageUrl = resolveMainImageUrl(variant.getVariantId());

            // Construir renglón inmutable
            SaleItem saleItem = SaleItem.builder()
                    .variant(variant)
                    .productName(variant.getProduct().getProductName())
                    .productDesc(variant.getProduct().getProductShortDesc())
                    .variantDesc(variant.getVariantDesc())
                    .variantSku(variant.getVariantSku())
                    .variantSize(variant.getVariantSize())
                    .colorName(variant.getColor().getColorName())
                    .imageUrl(mainImageUrl)
                    .unitPrice(variant.getVariantPrice())
                    .quantity(itemReq.quantity())
                    .subtotal(lineSubtotal)
                    .build();

            saleItems.add(saleItem);
        }

        // Calcular importes con políticas comerciales
        SaleCalculationResult calc = this.salePolicyService.calculateSaleTotals(
                totalQuantity,
                calculatedSubtotal,
                request.shippingCost(),
                buyerRole
        );

        // Construir y persistir la entidad cabecera Sale
        Sale sale = Sale.builder()
                .user(buyer)
                .customerName(request.customerName().trim())
                .customerEmail(request.customerEmail().trim().toLowerCase())
                .customerPhone(request.customerPhone().trim())
                .customerNotes(request.customerNotes())
                .shippingAddress(request.shippingAddress().trim())
                .saleQuantity(calc.totalQuantity())
                .saleSubtotal(calc.subtotal())
                .saleDiscountAmount(calc.discountAmount())
                .shippingCost(calc.shippingCost())
                .saleTotal(calc.total())
                .saleStatus(SaleStatus.PENDIENTE)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(PaymentStatus.PENDIENTE)
                .build();

        for (SaleItem item : saleItems) {
            sale.addItem(item);
        }

        Sale savedSale = this.iSaleRepository.save(sale);

        // Retornar DTO de respuesta
        return mapToSaleResponse(savedSale, null);
    }

    /**
     * Consulta pública segura mediante Magic Link / Token de seguimiento para invitados.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByTrackingToken(UUID trackingToken) {
        Sale sale = this.iSaleRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new SaleNotFoundException("{ Token de seguimiento: %s }".formatted(trackingToken)));

        return mapToSaleResponse(sale, null);
    }

    /**
     * Consulta de orden por ID para el usuario propietario autenticado.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByIdForUser(UUID saleId, UUID userId) {
        Sale sale = this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("{ ID: %s }" + saleId));

        if (sale.getUser() == null || !sale.getUser().getUserId().equals(userId))
            throw new InvalidCredentialsException("No tiene permisos para ver esta orden o la misma no le pertenece.");

        return mapToSaleResponse(sale, null);
    }

    /**
     * Historial paginado de órdenes del usuario registrado.
     */
    @Transactional(readOnly = true)
    public Page<SaleSummaryResponse> getMyOrders(UUID userId, Pageable pageable) {
        return this.iSaleRepository
                .findAllByUser_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToSaleSummaryResponse);
    }

    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================

    private String resolveMainImageUrl(Long variantId) {
        return this.imageService.getPrimaryByVariantId(variantId)
                .map(Image::getImageUrl)
                .orElse("https://via.placeholder.com/300x400?text=Sin+Imagen");
    }

    private SaleResponse mapToSaleResponse(Sale sale, String mpInitPoint) {
        List<SaleItemResponse> itemResponses = sale.getItems().stream()
                .map(item -> SaleItemResponse.builder()
                        .saleItemId(item.getSaleItemId())
                        .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                        .productName(item.getProductName())
                        .productDesc(item.getProductDesc())
                        .variantDesc(item.getVariantDesc())
                        .variantSku(item.getVariantSku())
                        .variantSize(item.getVariantSize())
                        .colorName(item.getColorName())
                        .imageUrl(item.getImageUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        return SaleResponse.builder()
                .saleId(sale.getSaleId())
                .userId(sale.getUser() != null ? sale.getUser().getUserId() : null)
                .trackingToken(sale.getTrackingToken())
                .customerName(sale.getCustomerName())
                .customerEmail(sale.getCustomerEmail())
                .customerPhone(sale.getCustomerPhone())
                .customerNotes(sale.getCustomerNotes())
                .shippingAddress(sale.getShippingAddress())
                .saleQuantity(sale.getSaleQuantity())
                .saleSubtotal(sale.getSaleSubtotal())
                .saleDiscountAmount(sale.getSaleDiscountAmount())
                .shippingCost(sale.getShippingCost())
                .saleTotal(sale.getSaleTotal())
                .saleShippedAt(sale.getSaleShippedAt())
                .saleDeliveredAt(sale.getSaleDeliveredAt())
                .saleStatus(sale.getSaleStatus())
                .paymentMethod(sale.getPaymentMethod())
                .paymentStatus(sale.getPaymentStatus())
                .internalNotes(sale.getInternalNotes())
                .mpPreferenceId(sale.getMpPreferenceId())
                .mpInitPoint(mpInitPoint)
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    private SaleSummaryResponse mapToSaleSummaryResponse(Sale sale) {
        return SaleSummaryResponse.builder()
                .saleId(sale.getSaleId())
                .userId(sale.getUser() != null ? sale.getUser().getUserId() : null)
                .trackingToken(sale.getTrackingToken())
                .customerName(sale.getCustomerName())
                .customerEmail(sale.getCustomerEmail())
                .saleQuantity(sale.getSaleQuantity())
                .saleTotal(sale.getSaleTotal())
                .saleStatus(sale.getSaleStatus())
                .paymentMethod(sale.getPaymentMethod())
                .paymentStatus(sale.getPaymentStatus())
                .createdAt(sale.getCreatedAt())
                .build();
    }
}