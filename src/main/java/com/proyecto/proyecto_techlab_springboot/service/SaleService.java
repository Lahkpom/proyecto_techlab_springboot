package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.sale.*;
import com.proyecto.proyecto_techlab_springboot.exception.common.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.exception.common.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.InvalidSaleStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.MissingTaxIdentificationException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.EmptySaleItemsException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.exception.variant.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.*;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.service.policy.SaleCalculationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que atiende al cliente final:
 *  - Maneja compras de usuarios registrados (aprovechando su carrito o items enviados) e invitados.
 *  - Aplica bloqueo pesimista sobre las variantes para evitar sobreventas.
 *  - Valida y descuenta stock atómicamente.
 *  - Congela el snapshot de cada variante (incluyendo su imagen de portada).
 *  - Invoca a SalePolicyService para calcular totales y descuentos.
 *  - Persiste la orden y expone las consultas por trackingToken (Magic Link) y por usuario logueado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final ISaleRepository iSaleRepository;

    private final MercadoPagoService    mercadoPagoService;
    private final SalePolicyService     salePolicyService;
    private final VariantService        variantService;
    private final ImageService          imageService;
    private final UserService           userService;

    @Value("${arca.anonymous-invoice-limit:340000.00}")
    private BigDecimal anonymousInvoiceLimit;

    private record ResolvedTaxData(TaxIdType type, String number) {}

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

        // Resolver y Validar Identificación Fiscal del Comprador
        ResolvedTaxData taxData = resolveAndValidateTaxData(
                buyer,
                buyerRole,
                request.customerTaxIdType(),
                request.customerTaxIdNumber(),
                calc.total()
        );

        // Determinar estado inicial según método de pago:
        // Efectivo contraentrega nace directamente en PREPARACION
        SaleStatus initialSaleStatus = (request.paymentMethod() == PaymentMethod.EFECTIVO)
                ? SaleStatus.PREPARACION
                : SaleStatus.PENDIENTE;

        // Construir y persistir la entidad cabecera Sale
        Sale sale = Sale.builder()
                .user(buyer)
                .customerName(request.customerName().trim())
                .customerEmail(request.customerEmail().trim().toLowerCase())
                .customerPhone(request.customerPhone().trim())
                .customerTaxIdType(taxData.type())
                .customerTaxIdNumber(taxData.number())
                .customerNotes(request.customerNotes())
                .shippingAddress(request.shippingAddress().trim())
                .saleQuantity(calc.totalQuantity())
                .saleSubtotal(calc.subtotal())
                .saleDiscountAmount(calc.discountAmount())
                .shippingCost(calc.shippingCost())
                .saleTotal(calc.total())
                .saleStatus(initialSaleStatus)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(PaymentStatus.PENDIENTE)
                .build();

        for (SaleItem item : saleItems) {
            sale.addItem(item);
        }

        Sale savedSale = this.iSaleRepository.save(sale);

        // Si es Mercado Pago, generar la preferencia (COMENTAR CUANDO SE HAGAN PRUEBAS PARA QUE NO MOLESTE)
        String mpInitPoint = null;
        if (request.paymentMethod() == PaymentMethod.MERCADO_PAGO) {
            MercadoPagoPreferenceResult prefResult = this.mercadoPagoService.createPreference(savedSale);
            savedSale.setMpPreferenceId(prefResult.preferenceId());
            mpInitPoint = prefResult.initPoint();
        }

        // return mapToSaleResponse(savedSale, null);
        return mapToSaleResponse(savedSale, mpInitPoint);
    }

    private ResolvedTaxData resolveAndValidateTaxData(
            Users       buyer,
            UserRole    buyerRole,
            TaxIdType   requestTaxType,
            String      requestTaxNumber,
            BigDecimal  saleTotal
    ) {
        // Datos cargados en el perfil de usuario registrado
        TaxIdType resolvedType = (buyer != null && buyer.getTaxIdType() != null)
                ? buyer.getTaxIdType()
                : requestTaxType;

        String resolvedNumber = (buyer != null && buyer.getTaxIdNumber() != null && !buyer.getTaxIdNumber().isBlank())
                ? buyer.getTaxIdNumber()
                : (requestTaxNumber != null ? requestTaxNumber.trim() : null);

        // Mayoristas deben tener CUIT obligatorio
        if (buyerRole == UserRole.WHOLESALER && (resolvedType != TaxIdType.CUIT || resolvedNumber == null || resolvedNumber.isBlank()))
            throw new MissingTaxIdentificationException(
                    "Para compras mayoristas es obligatorio registrar un CUIT tributario válido."
            );

        // Compras que superan el límite de facturación anónima de ARCA
        if (saleTotal.compareTo(this.anonymousInvoiceLimit) >= 0 && (resolvedType == null || resolvedNumber == null || resolvedNumber.isBlank()))
            throw new MissingTaxIdentificationException(
                    "Por regulaciones fiscales de ARCA, las operaciones que superan los $%s requieren identificación obligatoria (DNI o CUIT)."
                            .formatted(this.anonymousInvoiceLimit)
            );

        return new ResolvedTaxData(resolvedType, resolvedNumber);
    }

    /**
     * Permite reintentar el pago con Mercado Pago para una orden propia.
     * Solo es válido si:
     *  - El método de pago original es MERCADO_PAGO.
     *  - El estado logístico sigue en PENDIENTE.
     *  - El estado del pago es PENDIENTE o RECHAZADO.
     */
    @Transactional
    public SaleResponse retryMercadoPagoPayment(UUID saleId, UUID userId) {
        Sale sale = this.getSaleByIdAndCheckBelongingOrThrow(saleId, userId);

        // Validar método de pago original
        if (sale.getPaymentMethod() != PaymentMethod.MERCADO_PAGO)
            throw new InvalidSaleStatusTransitionException(
                    "Solo se puede generar un link de Mercado Pago para órdenes configuradas con dicho método de pago. Método actual: " + sale.getPaymentMethod()
            );
        
        // Validar estado comercial de la venta
        if (sale.getSaleStatus() != SaleStatus.PENDIENTE)
            throw new InvalidSaleStatusTransitionException(
                    "No es posible reintentar el pago. La orden se encuentra en estado logístico: " + sale.getSaleStatus()
            );

        // Validar estado financiero actual
        PaymentStatus currentPaymentStatus = sale.getPaymentStatus();
        if (currentPaymentStatus != PaymentStatus.PENDIENTE && currentPaymentStatus != PaymentStatus.RECHAZADO)
            throw new InvalidSaleStatusTransitionException(
                    "No es posible reintentar el cobro. El estado del pago actual es: " + currentPaymentStatus
            );

        // Crear nueva preferencia en MP y actualizar entidad administrada
        MercadoPagoPreferenceResult prefResult = this.mercadoPagoService.createPreference(sale);
        sale.setMpPreferenceId(prefResult.preferenceId());

        return mapToSaleResponse(sale, prefResult.initPoint());
    }

    /**
     * Consulta pública segura mediante Magic Link / Token de seguimiento para invitados.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByTrackingToken(UUID trackingToken) {
        Sale sale = this.internalGetSaleByTrackingTokenEntityOrThrow(trackingToken);

        return mapToSaleResponse(sale, null);
    }

    /**
     * Consulta de orden por ID para el usuario propietario autenticado.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByIdForUser(UUID saleId, UUID userId) {
        Sale sale = this.getSaleByIdAndCheckBelongingOrThrow(saleId, userId);

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

    /**
     * Permite al cliente comprador cancelar su propia orden mientras siga en estado PENDIENTE.
     * Restituye automáticamente el stock comprometido.
     */
    @Transactional
    public SaleResponse cancelMySale(UUID saleId, UUID userId) {
        Sale sale = this.getSaleByIdAndCheckBelongingOrThrow(saleId, userId);

        // Validar que la orden siga en un estado cancelable por el cliente
        SaleStatus currentStatus = sale.getSaleStatus();
        if (!List.of(SaleStatus.PENDIENTE, SaleStatus.PREPARACION).contains(currentStatus))
            throw new InvalidSaleStatusTransitionException(
                    "Solo se pueden cancelar pedidos en estado PENDIENTE o PREPARACION. El pedido actual se encuentra en: %s. Comuníquese para soporte."
                    .formatted(currentStatus)
            );

        // Restituir el stock de las variantes
        this.restoreVariantStock(sale.getItems());

        // Actualizar estado
        sale.setSaleStatus(SaleStatus.CANCELADO);
        sale.setUpdatedAt(Instant.now());
        sale.setUpdatedBy(userId);

        return mapToSaleResponse(sale, null);
    }

    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================

    public Sale getSaleByIdAndCheckBelongingOrThrow(UUID saleId, UUID userId) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        // Validar que la orden pertenezca al usuario autenticado
        if (sale.getUser() == null || !sale.getUser().getUserId().equals(userId))
            throw new AccessDeniedException("No tiene permisos para modificar esta orden o la misma no le pertenece.");

        return sale;
    }

    /**
     * Recupera la entidad administrada Sale para clientes invitados mediante el trackingToken.
     */
    @Transactional(readOnly = true)
    public Sale getSaleByTrackingTokenEntityOrThrow(UUID trackingToken) {
        return this.internalGetSaleByTrackingTokenEntityOrThrow(trackingToken);
    }

    /**
     * Pone la orden en estado de disputa/reclamo posventa sin alterar stock ni importes.
     */
    @Transactional
    public void markSaleAsDisputed(UUID saleId) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setSaleStatus(SaleStatus.RECLAMO_ABIERTO);
        sale.setUpdatedAt(Instant.now());

        String auditNote = "\n[POSVENTA]: Solicitud de devolución ingresada. Orden retenida en RECLAMO_ABIERTO.";
        String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() : "";
        sale.setInternalNotes(currentNotes + auditNote);
    }

    /**
     * Cierra formalmente la orden cuando la devolución concluyó su liquidación física y monetaria.
     */
    @Transactional
    public void closeSaleWithReturn(UUID saleId) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setSaleStatus(SaleStatus.CERRADO_CON_DEVOLUCION);
        sale.setUpdatedAt(Instant.now());
    }

    /**
     * Revierte el estado de la venta desde RECLAMO_ABIERTO hacia ENTREGADO
     * ante desistimiento del cliente o rechazo total de la inspección física.
     */
    @Transactional
    public void revertSaleFromDispute(UUID saleId, String reason) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setSaleStatus(SaleStatus.ENTREGADO);
        sale.setUpdatedAt(Instant.now());

        String auditNote = "\n[POSVENTA - REVERSIÓN]: Orden reincorporada a ENTREGADO. Motivo: " + reason;
        String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() : "";
        sale.setInternalNotes(currentNotes + auditNote);
    }

    /**
     * Marca el cobro de la venta en REEMBOLSO_PENDIENTE tras aprobarse la inspección de la mercadería.
     */
    @Transactional
    public void markPaymentAsPendingRefund(UUID saleId, BigDecimal amountToRefund) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setPaymentStatus(PaymentStatus.REEMBOLSO_PENDIENTE);
        sale.setUpdatedAt(Instant.now());

        String auditNote = "\n[POSVENTA - REEMBOLSO]: Inspección aprobada. Pago en REEMBOLSO_PENDIENTE por un monto de $" + amountToRefund;
        String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() : "";
        sale.setInternalNotes(currentNotes + auditNote);
    }

    /**
     * Completa formalmente el ciclo contable de la orden tras liquidarse el reembolso.
     * Mueve el estado de pago a REEMBOLSADO y la orden a CERRADO_CON_DEVOLUCION.
     */
    @Transactional
    public void completeSaleRefund(UUID saleId, BigDecimal refundedAmount, String auditDetail, UUID operatorId) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setPaymentStatus(PaymentStatus.REEMBOLSADO);
        sale.setSaleStatus(SaleStatus.CERRADO_CON_DEVOLUCION);
        sale.setUpdatedAt(Instant.now());
        sale.setUpdatedBy(operatorId);

        String auditNote = "%n[POSVENTA - REEMBOLSO LIQUIDADO]: Reintegro completado por $%s. Detalle: %s"
                .formatted(refundedAmount, auditDetail);
        String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() : "";
        sale.setInternalNotes(currentNotes + auditNote);

        log.info("Venta { ID: {} } liquidada financieramente: PaymentStatus=REEMBOLSADO, SaleStatus=CERRADO_CON_DEVOLUCION", saleId);
    }

    private Sale getSaleByIdOrElseThrow(UUID saleId) {
        return this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("{ ID: %s }".formatted(saleId)));
    }

    private Sale internalGetSaleByTrackingTokenEntityOrThrow(UUID trackingToken) {
        return this.iSaleRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new SaleNotFoundException("{ Token de seguimiento: %s }".formatted(trackingToken)));
    }

    private String resolveMainImageUrl(Long variantId) {
        return this.imageService.getPrimaryByVariantId(variantId)
                .map(Image::getImageUrl)
                .orElse("https://via.placeholder.com/300x400?text=Sin+Imagen");
    }

    private void restoreVariantStock(List<SaleItem> items) {
        Map<Long, Integer> stockMap = items.stream()
                .filter(item -> item.getVariant() != null)
                .collect(
                    Collectors.toMap(
                        item -> item.getVariant().getVariantId(),
                        SaleItem::getQuantity,
                        Integer::sum // Si la misma variante estuviera repetida, acumula las cantidades
                    )
                );

        this.variantService.restoreStock(stockMap);
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