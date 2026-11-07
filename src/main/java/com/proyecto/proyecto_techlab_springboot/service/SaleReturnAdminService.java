package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.salereturn.*;
import com.proyecto.proyecto_techlab_springboot.exception.salereturn.InvalidReturnStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.exception.salereturn.SaleReturnNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.*;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleReturnRepository;
import com.proyecto.proyecto_techlab_springboot.service.fiscal.FiscalReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * El servicio respeta el principio de arquitectura:
 *  - Gobierna la máquina de estados operativa.
 *  - Aplica el recálculo proporcional del reembolso si hay prendas rechazadas en el peritaje.
 *  - Reingresa al stock físico únicamente las prendas marcadas con restockApproved = true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleReturnAdminService {

    private final ISaleReturnRepository iSaleReturnRepository;
    private final SaleService           saleService;
    private final VariantService        variantService;
    private final UserService           userService;
    private final MercadoPagoService    mercadoPagoService;
    private final FiscalReceiptService  fiscalReceiptService;


    // Matriz de transiciones permitidas para la gestión del expediente
    private static final Map<ReturnStatus, Set<ReturnStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReturnStatus.SOLICITADA         , Set.of(ReturnStatus.EN_TRANSITO           , ReturnStatus.RECIBIDO_EN_CONTROL, ReturnStatus.CANCELADA),
            ReturnStatus.EN_TRANSITO        , Set.of(ReturnStatus.RECIBIDO_EN_CONTROL   , ReturnStatus.CANCELADA),
            ReturnStatus.RECIBIDO_EN_CONTROL, Set.of(ReturnStatus.APROBADA              , ReturnStatus.RECHAZADA),
            ReturnStatus.APROBADA           , Set.of(), // Estado terminal de inspección
            ReturnStatus.RECHAZADA          , Set.of(), // Estado terminal de inspección
            ReturnStatus.CANCELADA          , Set.of()  // Estado terminal
    );

    /**
     * Búsqueda dinámica multicriterio paginada de devoluciones para el panel administrativo.
     */
    @Transactional(readOnly = true)
    public Page<SaleReturnSummaryResponse> searchReturns(
            ReturnStatus    returnStatus,
            ReturnReason    returnReason,
            String          customerEmail,
            Instant         startDate,
            Instant         endDate,
            Pageable        pageable
    ) {
        return this.iSaleReturnRepository.searchReturnsAdmin(
                returnStatus,
                returnReason,
                customerEmail,
                startDate,
                endDate,
                pageable
        ).map(this::mapToSummaryResponse);
    }

    /**
     * Consulta el expediente completo de una devolución por su identificador único.
     */
    @Transactional(readOnly = true)
    public SaleReturnResponse getReturnDetail(UUID returnId) {
        SaleReturn saleReturn = this.findReturnWithDetailsOrThrow(returnId);
        return mapToResponse(saleReturn);
    }

    /**
     * Transiciona el estado logístico del expediente (ej: asignación de guía de correo, arribo a depósito).
     */
    @Transactional
    public SaleReturnResponse updateReturnStatus(UUID returnId, SaleReturnStatusUpdateRequest request, UUID operatorId) {
        SaleReturn      saleReturn      = this.findReturnWithDetailsOrThrow(returnId);
        ReturnStatus    currentStatus   = saleReturn.getReturnStatus();
        ReturnStatus    targetStatus    = request.newStatus();

        if (currentStatus == targetStatus) return mapToResponse(saleReturn);

        // Validar que la transición de estados esté permitida
        validateStatusTransition(currentStatus, targetStatus);

        // Impedir saltos directos a APROBADA o RECHAZADA sin peritaje físico
        if (List.of(ReturnStatus.APROBADA, ReturnStatus.RECHAZADA).contains(targetStatus))
            throw new InvalidReturnStatusTransitionException(
                    "No es posible pasar a '%s' de forma manual. Debe realizar la inspección física formal mediante el endpoint de peritaje."
                            .formatted(targetStatus)
            );

        // Aplicar guía de seguimiento si se provee
        if (request.returnShippingGuide() != null && !request.returnShippingGuide().isBlank())
            saleReturn.setReturnShippingGuide(request.returnShippingGuide().trim());

        // Efectos colaterales según estado destino
        if (targetStatus == ReturnStatus.CANCELADA) {
            saleReturn.setClosedAt(Instant.now());
            // Revertir la orden en SaleService a ENTREGADO
            this.saleService.revertSaleFromDispute(saleReturn.getSale().getSaleId(), "Devolución cancelada por el cliente/administración.");
        }

        saleReturn.setReturnStatus(targetStatus);
        saleReturn.setUpdatedAt(Instant.now());

        log.info("Estado de devolución actualizado exitosamente { returnId: {}, anterior: {}, nuevo: {}, operador: {} }",
                returnId, currentStatus, targetStatus, operatorId);

        return mapToResponse(saleReturn);
    }

    /**
     * Realiza el peritaje físico de calidad prenda por prenda tras abrir el paquete en taller.
     */
    @Transactional
    public SaleReturnResponse inspectReturn(UUID returnId, SaleReturnInspectionRequest request, UUID operatorId) {
        SaleReturn saleReturn = this.findReturnWithDetailsOrThrow(returnId);

        // Validar precondición física
        if (saleReturn.getReturnStatus() != ReturnStatus.RECIBIDO_EN_CONTROL)
            throw new InvalidReturnStatusTransitionException(
                    "Solo se pueden peritar devoluciones que se encuentren en estado RECIBIDO_EN_CONTROL. Estado actual: %s"
                            .formatted(saleReturn.getReturnStatus())
            );

        Users inspector = this.userService.findUserByIdOrThrow(operatorId);

        // Mapear renglones para asignar veredicto individual
        Map<Long, SaleReturnItem> returnItemMap = new HashMap<>();
        for (SaleReturnItem item : saleReturn.getItems()) {
            returnItemMap.put(item.getReturnItemId(), item);
        }

        int                 approvedItemsCount      = 0;
        BigDecimal          approvedItemsSubtotal   = BigDecimal.ZERO;
        Map<Long, Integer>  stockToRestore          = new HashMap<>();

        for (SaleReturnItemInspectionRequest itemVerdict : request.inspectedItems()) {
            SaleReturnItem returnItem = returnItemMap.get(itemVerdict.returnItemId());

            if (returnItem == null)
                throw new IllegalArgumentException(
                        "El ítem { returnItemId: %d } no pertenece a la devolución { returnId: %s }"
                                .formatted(itemVerdict.returnItemId(), returnId)
                );

            returnItem.setRestockApproved(itemVerdict.restockApproved());
            returnItem.setRejectionReason(itemVerdict.rejectionReason());

            if (Boolean.TRUE.equals(itemVerdict.restockApproved())) {
                approvedItemsCount += returnItem.getQuantity();

                SaleItem    si              = returnItem.getSaleItem();
                BigDecimal  lineSubtotal    = si.getUnitPrice().multiply(BigDecimal.valueOf(returnItem.getQuantity()));

                approvedItemsSubtotal = approvedItemsSubtotal.add(lineSubtotal);

                // Si la variante viva aún existe en el catálogo, acumularla para reingreso a stock físico
                if (si.getVariant() != null)
                    stockToRestore.merge(si.getVariant().getVariantId(), returnItem.getQuantity(), Integer::sum);
            }
        }

        // Asentar datos del peritaje en la cabecera
        Instant now = Instant.now();
        saleReturn.setInspectionNotes(request.inspectionNotes().trim());
        saleReturn.setInspectionEvidencePath(request.inspectionEvidencePath());
        saleReturn.setInspectedBy(inspector);
        saleReturn.setInspectedAt(now);
        saleReturn.setUpdatedAt(now);
        saleReturn.setClosedAt(now);

        Sale sale = saleReturn.getSale();

        // Dictamen Global del Expediente
        if (approvedItemsCount == 0) {
            // RECHAZO TOTAL: Todas las prendas tenían signos de uso o roturas ajenas
            saleReturn.setReturnStatus(ReturnStatus.RECHAZADA);
            saleReturn.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            // La venta vuelve al estado ENTREGADO
            this.saleService.revertSaleFromDispute(sale.getSaleId(), "Inspección física rechazada en su totalidad por control de calidad.");

            log.warn("Devolución { ID: {} } RECHAZADA en su totalidad. No califica para reembolso ni reingreso de stock.", returnId);
        } else {
            // APROBADA (Total o Parcial)
            saleReturn.setReturnStatus(ReturnStatus.APROBADA);

            // Reingreso selectivo de stock físico mediante VariantService
            if (!stockToRestore.isEmpty()) this.variantService.restoreStock(stockToRestore);

            // Recalcular reembolso proporcional según prendas aprobadas
            BigDecimal finalRefund = calculateApprovedRefund(sale, saleReturn, approvedItemsCount, approvedItemsSubtotal);
            saleReturn.setRefundAmount(finalRefund);

            // Transicionar el cobro en SaleService a REEMBOLSO_PENDIENTE si corresponde reintegro de dinero
            if (saleReturn.getReturnResolution() == ReturnResolution.REEMBOLSO_DINERO && finalRefund.compareTo(BigDecimal.ZERO) > 0)
                this.saleService.markPaymentAsPendingRefund(sale.getSaleId(), finalRefund);

            log.info("Devolución { ID: {} } APROBADA. Unidades reincorporadas a stock: {}, Reembolso final: ${}",
                    returnId, approvedItemsCount, finalRefund);
        }

        return mapToResponse(saleReturn);
    }

    /**
     * Procesa la liquidación financiera de una devolución aprobada:
     * - Si pagó con MERCADO_PAGO: llama a la API de MP con idempotencia.
     * - Si pagó con TRANSFERENCIA o EFECTIVO: exige comprobante manual de conciliación bancaria.
     * Notifica a SaleService para transicionar a REEMBOLSADO y CERRADO_CON_DEVOLUCION.
     */
    @Transactional
    public SaleReturnResponse processRefund(
            UUID                    returnId,
            SaleReturnRefundRequest request,
            UUID                    operatorId
    ) {
        SaleReturn saleReturn = this.findReturnWithDetailsOrThrow(returnId);

        // Validar que el expediente califique para liquidación monetaria
        if (saleReturn.getReturnStatus() != ReturnStatus.APROBADA)
            throw new InvalidReturnStatusTransitionException(
                    "Solo se pueden reembolsar devoluciones que se encuentren en estado APROBADA. Estado actual: %s"
                            .formatted(saleReturn.getReturnStatus())
            );

        if (saleReturn.getReturnResolution() != ReturnResolution.REEMBOLSO_DINERO)
            throw new IllegalStateException(
                    "Esta devolución tiene resolución '%s'. No requiere liquidación monetaria."
                            .formatted(saleReturn.getReturnResolution())
            );

        if (saleReturn.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("El monto liquidable para esta devolución es $0.00.");

        Sale sale = saleReturn.getSale();

        if (sale.getPaymentStatus() == PaymentStatus.REEMBOLSADO)
            throw new IllegalStateException("El cobro de la orden vinculada ya figura como REEMBOLSADO.");

        String auditDetail;

        // Liquidación según forma de pago original
        if (sale.getPaymentMethod() == PaymentMethod.MERCADO_PAGO) {
            if (sale.getMpPaymentId() == null || sale.getMpPaymentId().isBlank())
                throw new IllegalStateException(
                        "La orden { ID: %s } fue pagada con Mercado Pago pero no registra un mpPaymentId válido para ejecutar el reembolso."
                                .formatted(sale.getSaleId())
                );

            Long mpPayId    = Long.valueOf(sale.getMpPaymentId());
            Long mpRefundId = this.mercadoPagoService.refundPayment(
                    mpPayId,
                    saleReturn.getRefundAmount(),
                    saleReturn.getReturnId()
            );

            auditDetail = "Mercado Pago Refund ID: %d (Pago MP: %d)".formatted(mpRefundId, mpPayId);

        } else {
            // TRANSFERENCIA o EFECTIVO: Conciliación manual bancaria
            if (request == null || request.transactionReference() == null || request.transactionReference().isBlank())
                throw new IllegalArgumentException(
                        "Para órdenes abonadas con %s es obligatorio ingresar el código o número de comprobante de la transferencia de reintegro."
                                .formatted(sale.getPaymentMethod())
                );

            auditDetail = "Conciliación bancaria manual. Ref: %s. Notas: %s"
                    .formatted(
                            request.transactionReference().trim(),
                            request.notes() != null ? request.notes().trim() : "Sin notas adicionales"
                    );
        }

        // Notificar a SaleService para actualizar estados y asentar auditoría
        this.saleService.completeSaleRefund(
                sale.getSaleId(),
                saleReturn.getRefundAmount(),
                auditDetail,
                operatorId
        );

        // DISPARO FISCAL: Emisión de Nota de Crédito C vinculada a la Factura C original
        this.fiscalReceiptService.emitCreditNoteForReturn(returnId, operatorId);

        // Actualizar marca temporal en el expediente de posventa
        saleReturn.setClosedAt(Instant.now());

        log.info("Reembolso de devolución { returnId: {} } liquidado exitosamente por el operador {}. Detalle: {}",
                returnId, operatorId, auditDetail);

        return mapToResponse(saleReturn);
    }

    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================

    private BigDecimal calculateApprovedRefund(
            Sale        sale,
            SaleReturn  saleReturn,
            int         approvedCount,
            BigDecimal  approvedSubtotal
    ) {
        if (saleReturn.getReturnResolution() == ReturnResolution.CAMBIO_PRODUCTO)
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Si se aprobaron todas las prendas de la orden original y es arrepentimiento legal, reintegra total con flete
        if (approvedCount == sale.getSaleQuantity() && saleReturn.getReturnReason() == ReturnReason.ARREPENTIMIENTO_LEGAL)
            return sale.getSaleTotal();

        // Si fue aprobación parcial: descuenta proporcionalmente el descuento original
        BigDecimal subtotalRatio = approvedSubtotal.divide(sale.getSaleSubtotal(), 4, RoundingMode.HALF_UP);
        BigDecimal proportionalDiscount = sale.getSaleDiscountAmount().multiply(subtotalRatio).setScale(2, RoundingMode.HALF_UP);

        return approvedSubtotal.subtract(proportionalDiscount).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(ReturnStatus current, ReturnStatus target) {
        Set<ReturnStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target))
            throw new InvalidReturnStatusTransitionException(current, target);
    }

    private SaleReturn findReturnWithDetailsOrThrow(UUID returnId) {
        return this.iSaleReturnRepository.findByIdWithDetails(returnId)
                .orElseThrow(() -> new SaleReturnNotFoundException("{ ID: %s }".formatted(returnId)));
    }

    private SaleReturnSummaryResponse mapToSummaryResponse(SaleReturn sr) {
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

    private SaleReturnResponse mapToResponse(SaleReturn sr) {
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
}