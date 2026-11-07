package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.salereturn.*;
import com.proyecto.proyecto_techlab_springboot.exception.salereturn.*;
import com.proyecto.proyecto_techlab_springboot.exception.user.UserNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.model.SaleReturn;
import com.proyecto.proyecto_techlab_springboot.model.SaleReturnItem;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleReturnItemRepository;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Este servicio gestiona el Botón de Arrepentimiento tanto para usuarios registrados como para invitados por token:
 *  - Valida la ventana legal de 10 días corridos (sale.getSaleDeliveredAt() + 10 días).
 *  - Impide reclamos duplicados sobre la misma venta.
 *  - Verifica que las cantidades solicitadas no excedan las prendas efectivamente compradas.
 *  - Calcula el monto de devolución con base económica real (devolviendo el total completo con flete si es arrepentimiento total, o proporcional si es parcial).
 *  - Invoca a saleService.markSaleAsDisputed(...) preservando el encapsulamiento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleReturnService {

    private static final int LEGAL_RETURN_WINDOW_DAYS = 10;
    private static final Set<ReturnStatus> ACTIVE_RETURN_STATUSES = Set.of(
            ReturnStatus.SOLICITADA,
            ReturnStatus.EN_TRANSITO,
            ReturnStatus.RECIBIDO_EN_CONTROL
    );
    private static final Set<ReturnStatus> TERMINAL_EXCLUDED_STATUSES = Set.of(
            ReturnStatus.RECHAZADA,
            ReturnStatus.CANCELADA
    );

    private final ISaleReturnRepository     iSaleReturnRepository;
    private final ISaleReturnItemRepository iSaleReturnItemRepository;
    private final SaleService               saleService;

    /**
     * Inicia una devolución para un usuario registrado autenticado.
     */
    @Transactional
    public SaleReturnResponse createReturnForUser(UUID saleId, SaleReturnCreateRequest request, UUID userId) {
        Sale sale = this.saleService.getSaleByIdAndCheckBelongingOrThrow(saleId, userId);
        return executeCreateReturn(sale, request);
    }

    /**
     * Inicia una devolución para una compra como invitado mediante su Magic Link (trackingToken).
     */
    @Transactional
    public SaleReturnResponse createReturnByTrackingToken(UUID trackingToken, SaleReturnCreateRequest request) {
        Sale sale = this.saleService.getSaleByTrackingTokenEntityOrThrow(trackingToken);
        return executeCreateReturn(sale, request);
    }

    /**
     * Consulta pública del estado del trámite de posventa mediante su propio trackingToken de devolución.
     */
    @Transactional(readOnly = true)
    public SaleReturnResponse getReturnByTrackingToken(UUID returnTrackingToken) {
        SaleReturn saleReturn = this.iSaleReturnRepository.findByTrackingToken(returnTrackingToken)
                .orElseThrow(() -> new SaleReturnNotFoundException("{ Token de seguimiento: %s }".formatted(returnTrackingToken)));

        return mapToReturnResponse(saleReturn);
    }

    /**
     * Historial de solicitudes de posventa del cliente autenticado.
     */
    @Transactional(readOnly = true)
    public Page<SaleReturnSummaryResponse> getMyReturns(UUID userId, Pageable pageable) {
        return this.iSaleReturnRepository
                .findAllBySale_User_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToReturnSummaryResponse);
    }

    public SaleReturn getSaleReturnByIdOrThrow(UUID returnId) {
        return this.iSaleReturnRepository.findById(returnId)
                .orElseThrow(() -> new SaleReturnNotFoundException("{ ID: %s }".formatted(returnId)));
    }

    // =========================================================================
    // Lógica Privada de Dominio
    // =========================================================================

    private SaleReturnResponse executeCreateReturn(Sale sale, SaleReturnCreateRequest request) {
        // Validar que la venta cumpla las condiciones de estado para iniciar la solicitud de devolución
        validateSaleEligibility(sale);

        // Validar que no exista un expediente en curso sobre esta misma venta
        // Una orden solo puede tener un trámite activo a la vez. Hasta que el trámite previo no concluya (sea aceptado o formalmente rechazado/cancelado), no se admite abrir otro.
        if (this.iSaleReturnRepository.existsBySale_SaleIdAndReturnStatusIn(sale.getSaleId(), ACTIVE_RETURN_STATUSES))
            throw new ActiveReturnAlreadyExistsException(sale.getSaleId());

        // Mapear renglones de la venta para acceso rápido
        // Permite validar de forma instantánea si el saleItemId que envió el cliente en el JSON realmente pertenecía a esta compra y no a otra orden distinta.
        Map<Long, SaleItem> saleItemMap = new HashMap<>();
        for (SaleItem si : sale.getItems()) {
            saleItemMap.put(si.getSaleItemId(), si);
        }

        // Validar unidades solicitadas contra historial de reclamos previos
        // Este cálculo permite devoluciones parciales escalonadas sin permitir jamás que devuelva 3 habiendo comprado 2.
        int                     requestedTotalItems     = 0;
        BigDecimal              claimedItemsSubtotal    = BigDecimal.ZERO;
        List<SaleReturnItem>    returnItems             = new ArrayList<>();

        for (SaleReturnItemRequest itemReq : request.items()) {
            SaleItem saleItem = saleItemMap.get(itemReq.saleItemId());
            if (saleItem == null)
                throw new IllegalArgumentException(
                        "El ítem { ID: %d } no pertenece a la orden { ID: %s }".formatted(itemReq.saleItemId(), sale.getSaleId())
                );

            // Unidades previamente reclamadas en trámites activos o aprobados
            int alreadyClaimed = this.iSaleReturnItemRepository
                    .countClaimedQuantityBySaleItemId(saleItem.getSaleItemId(), TERMINAL_EXCLUDED_STATUSES);

            int availableToReturn = saleItem.getQuantity() - alreadyClaimed;

            if (itemReq.quantity() > availableToReturn)
                throw new ReturnItemQuantityExceededException(
                        saleItem.getSaleItemId(), itemReq.quantity(), availableToReturn
                );

            // Construir el ítem de devolución
            SaleReturnItem returnItem = SaleReturnItem.builder()
                    .saleItem(saleItem)
                    .quantity(itemReq.quantity())
                    .restockApproved(null) // Queda pendiente de la inspección física en taller
                    .build();

            returnItems.add(returnItem);
            requestedTotalItems += itemReq.quantity();

            BigDecimal lineValue = saleItem.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            claimedItemsSubtotal = claimedItemsSubtotal.add(lineValue);
        }

        // Determinar si el flete inverso corre por cuenta de la tienda
        // Obligatorio por ley en Arrepentimiento (Art. 34 Ley 24.240) y en Fallas de fábrica
        boolean isStoreCoveredShipping = (request.returnReason() == ReturnReason.ARREPENTIMIENTO_LEGAL
                || request.returnReason() == ReturnReason.DEFECTO_FABRICACION);

        // Calcular monto exacto a reintegrar
        BigDecimal calculatedRefund = calculateRefundAmount(
                sale,
                requestedTotalItems,
                claimedItemsSubtotal,
                request.returnReason(),
                request.returnResolution()
        );

        // Construir cabecera de devolución
        SaleReturn saleReturn = SaleReturn.builder()
                .sale(sale)
                .returnReason(request.returnReason())
                .returnResolution(request.returnResolution())
                .returnStatus(ReturnStatus.SOLICITADA)
                .shippingCoveredByStore(isStoreCoveredShipping)
                .refundAmount(calculatedRefund)
                .customerNotes(request.customerNotes().trim())
                .build();

        for (SaleReturnItem item : returnItems) {
            saleReturn.addItem(item);
        }

        SaleReturn savedReturn = this.iSaleReturnRepository.save(saleReturn);

        // Notificar a SaleService para transicionar la orden a RECLAMO_ABIERTO
        this.saleService.markSaleAsDisputed(sale.getSaleId());

        log.info("Expediente de devolución creado { returnId: {}, saleId: {}, motivo: {}, montoReembolso: {} }",
                savedReturn.getReturnId(), sale.getSaleId(), savedReturn.getReturnReason(), savedReturn.getRefundAmount()
        );

        return mapToReturnResponse(savedReturn);
    }

    private void validateSaleEligibility(Sale sale) {
        if (sale.getSaleStatus() != SaleStatus.ENTREGADO)
            throw new SaleNotEligibleForReturnException(sale.getSaleId(), sale.getSaleStatus());

        // Si no tiene marca de tiempo no se puede calcular el plazo
        if (sale.getSaleDeliveredAt() == null)
            throw new SaleNotEligibleForReturnException(
                    "La orden { ID: %s } figura como ENTREGADO pero carece de marca de tiempo saleDeliveredAt."
                            .formatted(sale.getSaleId())
            );

        // Validar ventana de 10 días corridos
        Instant expirationThreshold = sale.getSaleDeliveredAt().plus(Duration.ofDays(LEGAL_RETURN_WINDOW_DAYS));
        if (Instant.now().isAfter(expirationThreshold))
            throw new ReturnWindowExpiredException(
                    sale.getSaleId(), sale.getSaleDeliveredAt(), LEGAL_RETURN_WINDOW_DAYS
            );
    }

    private BigDecimal calculateRefundAmount(
            Sale                sale,
            int                 requestedTotalQuantity,
            BigDecimal          claimedItemsSubtotal,
            ReturnReason        reason,
            ReturnResolution    resolution
    ) {
        if (resolution == ReturnResolution.CAMBIO_PRODUCTO)
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Arrepentimiento Total: devuelve el total congelado original (incluyendo flete original)
        boolean isTotalReturn = (requestedTotalQuantity == sale.getSaleQuantity());
        if (isTotalReturn && reason == ReturnReason.ARREPENTIMIENTO_LEGAL)
            return sale.getSaleTotal();

        // Devolución Parcial: prorratea el descuento que haya tenido la venta original
        // Si el cliente compró con un descuento mayorista o promoción por cantidad, no se reembolsa el precio de lista unitario completo; se reintegra el valor neto que efectivamente pagó por esa prenda.
        // Tratamiento del flete: En devoluciones parciales no se reintegra el costo de envío original, porque el servicio de flete fue efectivamente aprovechado para llevarle las prendas con las que sí se quedó.
        BigDecimal subtotalRatio        = claimedItemsSubtotal.divide(sale.getSaleSubtotal(), 4, RoundingMode.HALF_UP);
        BigDecimal proportionalDiscount = sale.getSaleDiscountAmount().multiply(subtotalRatio).setScale(2, RoundingMode.HALF_UP);

        return claimedItemsSubtotal.subtract(proportionalDiscount).setScale(2, RoundingMode.HALF_UP);
    }

    private SaleReturnResponse mapToReturnResponse(SaleReturn sr) {
        List<SaleReturnItemResponse> itemResponses = sr.getItems().stream()
                .map(item -> {
                    SaleItem si = item.getSaleItem();
                    return SaleReturnItemResponse.builder()
                            .returnItemId(item.getReturnItemId())
                            .saleItemId(si.getSaleItemId())
                            .variantId(si.getVariant() != null ? si.getVariant().getVariantId() : null)
                            .productName(si.getProductName())
                            .variantDesc(si.getVariantDesc())
                            .variantSku(si.getVariantSku())
                            .variantSize(si.getVariantSize())
                            .colorName(si.getColorName())
                            .imageUrl(si.getImageUrl())
                            .unitPrice(si.getUnitPrice())
                            .quantity(item.getQuantity())
                            .subtotal(si.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .restockApproved(item.getRestockApproved())
                            .rejectionReason(item.getRejectionReason())
                            .createdAt(item.getCreatedAt())
                            .build();
                })
                .toList();

        return SaleReturnResponse.builder()
                .returnId(sr.getReturnId())
                .saleId(sr.getSale().getSaleId())
                .trackingToken(sr.getTrackingToken())
                .returnReason(sr.getReturnReason())
                .returnResolution(sr.getReturnResolution())
                .returnStatus(sr.getReturnStatus())
                .shippingCoveredByStore(sr.getShippingCoveredByStore())
                .returnShippingGuide(sr.getReturnShippingGuide())
                .refundAmount(sr.getRefundAmount())
                .customerNotes(sr.getCustomerNotes())
                .inspectionNotes(sr.getInspectionNotes())
                .inspectionEvidencePath(sr.getInspectionEvidencePath())
                .inspectedBy(sr.getInspectedBy() != null ? sr.getInspectedBy().getUserId() : null)
                .inspectedAt(sr.getInspectedAt())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .closedAt(sr.getClosedAt())
                .items(itemResponses)
                .build();
    }

    private SaleReturnSummaryResponse mapToReturnSummaryResponse(SaleReturn sr) {
        return SaleReturnSummaryResponse.builder()
                .returnId(sr.getReturnId())
                .saleId(sr.getSale().getSaleId())
                .trackingToken(sr.getTrackingToken())
                .customerName(sr.getSale().getCustomerName())
                .customerEmail(sr.getSale().getCustomerEmail())
                .returnReason(sr.getReturnReason())
                .returnResolution(sr.getReturnResolution())
                .returnStatus(sr.getReturnStatus())
                .refundAmount(sr.getRefundAmount())
                .createdAt(sr.getCreatedAt())
                .closedAt(sr.getClosedAt())
                .build();
    }
}