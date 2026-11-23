package com.proyecto.proyecto_techlab_springboot.sale.service;

import com.proyecto.proyecto_techlab_springboot.common.exception.AccessDeniedException;
import com.proyecto.proyecto_techlab_springboot.common.exception.InsufficientStockException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.InvalidSaleStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.InvalidShippingPaymentCombinationException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.MissingTaxIdentificationException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.EmptySaleItemsException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.sale.model.*;
import com.proyecto.proyecto_techlab_springboot.variant.exception.VariantNotFoundException;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.image.dto.ImageKey;
import com.proyecto.proyecto_techlab_springboot.image.service.ImageService;
import com.proyecto.proyecto_techlab_springboot.sale.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.sale.client.IMercadoPagoClient;
import com.proyecto.proyecto_techlab_springboot.sale.dto.*;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleCalculationResult;
import com.proyecto.proyecto_techlab_springboot.sale.mapper.SaleMapper;
import com.proyecto.proyecto_techlab_springboot.fiscal.config.ArcaProperties;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.exception.InvalidShippingDestinationException;
import com.proyecto.proyecto_techlab_springboot.shipping.service.ShippingService;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final ISaleRepository iSaleRepository;
    private final SaleMapper saleMapper;

    private final IMercadoPagoClient    iMercadoPagoClient;
    private final SalePolicyService     salePolicyService;
    private final VariantService variantService;
    private final ImageService imageService;
    private final UserService userService;
    private final ShippingService shippingService;
    private final ArcaProperties arcaProperties;

    private record ResolvedTaxData(TaxIdType type, String number) {}

    /**
     * Orquestación transaccional unificada para checkout (registrado o invitado).
     */
    @Transactional
    public SaleResponse createSale(CheckoutRequest request, UUID authenticatedUserId) {
        if (request.items() == null || request.items().isEmpty())
            throw new EmptySaleItemsException();

        ShippingQuoteResponse shippingQuote = this.shippingService.quoteShipping(
                request.isPickup(),
                request.shippingProvince(),
                request.shippingLocality(),
                request.shippingPostalCode()
        );

        if (!request.isPickup() && (request.shippingAddress() == null || request.shippingAddress().isBlank())) {
            throw new InvalidShippingDestinationException(
                    "La dirección (calle y altura) es obligatoria para envíos a domicilio."
            );
        }

        if (request.paymentMethod() == PaymentMethod.EFECTIVO && !shippingQuote.cashPaymentAllowed()) {
            throw new InvalidShippingPaymentCombinationException(
                    "El pago en efectivo no está habilitado para envíos por correo nacional."
            );
        }

        // Resolver usuario y rol del comprador
        Users buyer       = null;
        UserRole buyerRole   = null;
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
        Map<ImageKey, String> primaryImages = this.imageService.getPrimaryImageUrls(
                lockedVariants.stream()
                        .map(variant -> new ImageKey(
                                variant.getProduct().getProductId(),
                                variant.getColor().getColorId()
                        ))
                        .toList()
        );

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
            String mainImageUrl = primaryImages.getOrDefault(
                    new ImageKey(variant.getProduct().getProductId(), variant.getColor().getColorId()),
                    "https://via.placeholder.com/300x400?text=Sin+Imagen"
            );

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
                shippingQuote.shippingCost(),
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

        // Determinar estado inicial según forma de pago:
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
                .shippingMethod(shippingQuote.shippingMethod())
                .shippingProvince(trimToNull(request.shippingProvince()))
                .shippingLocality(trimToNull(request.shippingLocality()))
                .shippingPostalCode(trimToNull(request.shippingPostalCode()))
                .shippingAddress(
                        request.shippingAddress() == null || request.shippingAddress().isBlank()
                                ? "Retiro en Estación Llavallol"
                                : request.shippingAddress().trim()
                )
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
            MercadoPagoPreferenceResult prefResult = this.iMercadoPagoClient.createPreference(savedSale);
            savedSale.setMpPreferenceId(prefResult.preferenceId());
            mpInitPoint = prefResult.initPoint();
        }

        // return saleMapper.toResponse(savedSale, null);
        return saleMapper.toResponse(savedSale, mpInitPoint);
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
        if (saleTotal.compareTo(this.arcaProperties.anonymousInvoiceLimit()) >= 0 && (resolvedType == null || resolvedNumber == null || resolvedNumber.isBlank()))
            throw new MissingTaxIdentificationException(
                    "Por regulaciones fiscales de ARCA, las operaciones que superan los $%s requieren identificación obligatoria (DNI o CUIT)."
                            .formatted(this.arcaProperties.anonymousInvoiceLimit())
            );

        return new ResolvedTaxData(resolvedType, resolvedNumber);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
        MercadoPagoPreferenceResult prefResult = this.iMercadoPagoClient.createPreference(sale);
        sale.setMpPreferenceId(prefResult.preferenceId());

        return saleMapper.toResponse(sale, prefResult.initPoint());
    }

    /**
     * Consulta pública segura mediante Magic Link / Token de seguimiento para invitados.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByTrackingToken(UUID trackingToken) {
        Sale sale = this.internalGetSaleByTrackingTokenEntityOrThrow(trackingToken);

        return saleMapper.toResponse(sale, null);
    }

    /**
     * Consulta de orden por ID para el usuario propietario autenticado.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleByIdForUser(UUID saleId, UUID userId) {
        Sale sale = this.getSaleByIdAndCheckBelongingOrThrow(saleId, userId);

        return saleMapper.toResponse(sale, null);
    }

    /**
     * Historial paginado de órdenes del usuario registrado.
     */
    @Transactional(readOnly = true)
    public Page<SaleSummaryResponse> getMyOrders(UUID userId, List<SaleStatus> statuses, Pageable pageable) {
        boolean filterStatuses = statuses != null && !statuses.isEmpty();
        List<SaleStatus> effectiveStatuses = filterStatuses ? statuses : List.of(SaleStatus.values());
        return this.iSaleRepository
                .findMyOrdersByStatuses(userId, filterStatuses, effectiveStatuses, pageable)
                .map(saleMapper::toSummaryResponse);
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
        this.variantService.restoreStockForSaleItems(sale.getItems());

        // Actualizar estado
        sale.setSaleStatus(SaleStatus.CANCELADO);
        sale.setUpdatedAt(Instant.now());
        sale.setUpdatedBy(userId);

        return saleMapper.toResponse(sale, null);
    }

    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================

    @Transactional(readOnly = true)
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

        String auditNote = "Solicitud de devolución ingresada. Orden retenida en RECLAMO_ABIERTO.";
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                "SISTEMA - Posventa",
                auditNote
        ));
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

        String auditNote = "Orden reincorporada a ENTREGADO. Motivo: " + reason;
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                "SISTEMA - Posventa",
                auditNote
        ));
    }

    /**
     * Marca el cobro de la venta en REEMBOLSO_PENDIENTE tras aprobarse la inspección de la mercadería.
     */
    @Transactional
    public void markPaymentAsPendingRefund(UUID saleId, BigDecimal amountToRefund) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setPaymentStatus(PaymentStatus.REEMBOLSO_PENDIENTE);
        sale.setUpdatedAt(Instant.now());

        String auditNote = "Inspección aprobada. Pago en REEMBOLSO_PENDIENTE por un monto de $" + amountToRefund;
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                "SISTEMA - Posventa",
                auditNote
        ));
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

        String auditNote = "Reintegro completado por $%s. Detalle: %s"
                .formatted(refundedAmount, auditDetail);
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                "SISTEMA - Posventa",
                auditNote
        ));

        log.info("Venta { ID: {} } liquidada financieramente: PaymentStatus=REEMBOLSADO, SaleStatus=CERRADO_CON_DEVOLUCION", saleId);
    }

    @Transactional
    public void markSaleAsFiscalRejected(UUID saleId, String rejectionReason) {
        Sale sale = this.getSaleByIdOrElseThrow(saleId);

        sale.setFiscalRejection(true);
        sale.setUpdatedAt(Instant.now());

        String auditNote = "El comprobante fue rechazado formalmente: " + rejectionReason;
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                "SISTEMA - Facturación ARCA",
                auditNote
        ));

        log.warn("Venta ID {} marcada con fiscalRejection = true. Motivo: {}", saleId, rejectionReason);
    }

    @Transactional(readOnly = true)
    public Sale getSaleByIdOrElseThrow(UUID saleId) {
        return this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("{ ID: %s }".formatted(saleId)));
    }

    private Sale internalGetSaleByTrackingTokenEntityOrThrow(UUID trackingToken) {
        return this.iSaleRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new SaleNotFoundException("{ Token de seguimiento: %s }".formatted(trackingToken)));
    }

    @Transactional(readOnly = true)
    public List<Sale> findUnbilledSales(PaymentStatus paymentStatus, FiscalReceiptType receiptType, Pageable pageable) {
        return this.iSaleRepository.findUnbilledSalesByPaymentStatus(paymentStatus, receiptType, pageable);
    }

}