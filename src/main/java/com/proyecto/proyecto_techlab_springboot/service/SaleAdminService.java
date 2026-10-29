package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.sale.*;
import com.proyecto.proyecto_techlab_springboot.exception.sale.InvalidSaleStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Este servicio centraliza la gestión operativa y logística para los administradores y operadores del sistema:
 *     Búsqueda multicriterio paginada: Filtra por estados (SaleStatus, PaymentStatus), email de cliente y rango de fechas.
 *     Máquina de estados estricta: Valida que las transiciones sean legales (evita por ejemplo que una orden CANCELADO o ENTREGADO pase a PREPARACION).
 *     Cancelación con restitución de stock: Si la orden se cancela antes de despacharse, reingresa las unidades vendidas al stock de cada variante usando bloqueo pesimista.
 *     Marcas temporales automáticas: Asigna saleShippedAt al pasar a ENVIADO, y saleDeliveredAt al pasar a ENTREGADO.
 *     Notas internas: Actualiza internal_notes registrando el operador que hizo la modificación en updatedBy.
 */
@Service
@RequiredArgsConstructor
public class SaleAdminService {

    private final ISaleRepository   iSaleRepository;
    private final VariantService    variantService;

    // Matriz de transiciones de ciclo de vida legales
    private static final Map<SaleStatus, Set<SaleStatus>> ALLOWED_TRANSITIONS = Map.of(
            SaleStatus.PENDIENTE,   Set.of(SaleStatus.PREPARACION, SaleStatus.CANCELADO),
            SaleStatus.PREPARACION, Set.of(SaleStatus.ENVIADO, SaleStatus.CANCELADO),
            SaleStatus.ENVIADO,     Set.of(SaleStatus.ENTREGADO, SaleStatus.CANCELADO),
            SaleStatus.ENTREGADO,   Set.of(SaleStatus.REEMBOLSADO),
            SaleStatus.CANCELADO,   Set.of(), // Estado terminal
            SaleStatus.REEMBOLSADO, Set.of()  // Estado terminal
    );

    /**
     * Búsqueda paginada multicriterio para el panel administrativo.
     */
    @Transactional(readOnly = true)
    public Page<SaleSummaryResponse> searchSales(
            SaleStatus      saleStatus,
            PaymentStatus   paymentStatus,
            String          customerEmail,
            Instant         startDate,
            Instant         endDate,
            Pageable        pageable
    ) {
        return this.iSaleRepository.searchSalesAdmin(
                saleStatus,
                paymentStatus,
                customerEmail,
                startDate,
                endDate,
                pageable
        ).map(this::mapToSaleSummaryResponse);
    }

    /**
     * Obtiene el detalle completo de cualquier orden por su ID.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleDetail(UUID saleId) {
        return mapToSaleResponse(this.findSaleByIdOrThrow(saleId));
    }

    /**
     * Avanza o cambia el estado logístico de una orden respetando:
     * 1. La máquina de estados propia de SaleStatus.
     * 2. Las restricciones cruzadas con PaymentMethod y PaymentStatus.
     */
    @Transactional
    public SaleResponse updateSaleStatus(UUID saleId, SaleStatusUpdateRequest request, UUID operatorId) {
        Sale        sale            = this.findSaleByIdOrThrow(saleId);
        SaleStatus  currentStatus   = sale.getSaleStatus();
        SaleStatus  targetStatus    = request.newStatus();

        if (currentStatus == targetStatus) return mapToSaleResponse(sale);

        // Validar máquina de estados logísticos
        this.validateTransition(currentStatus, targetStatus);

        // Validaciones cruzadas de negocio
        validateSaleStatusCrossRules(sale, targetStatus);

        // Manejo de stock ante cancelación
        if (targetStatus == SaleStatus.CANCELADO && currentStatus != SaleStatus.ENTREGADO)
            this.restoreVariantStock(sale.getItems());

        // Efectivo entregado aprueba el cobro
        if (targetStatus == SaleStatus.ENTREGADO && sale.getPaymentMethod() == PaymentMethod.EFECTIVO)
            sale.setPaymentStatus(PaymentStatus.APROBADO);

        // Si se cancela una orden ya cobrada, pasa a REEMBOLSADO
        if (targetStatus == SaleStatus.CANCELADO && sale.getPaymentStatus() == PaymentStatus.APROBADO)
            sale.setPaymentStatus(PaymentStatus.REEMBOLSADO);

        // Marcas temporales logísticas
        Instant now = Instant.now();
        if (targetStatus == SaleStatus.ENVIADO && sale.getSaleShippedAt() == null) {
            sale.setSaleShippedAt(now);
        } else if (targetStatus == SaleStatus.ENTREGADO && sale.getSaleDeliveredAt() == null) {
            sale.setSaleDeliveredAt(now);
        }

        sale.setSaleStatus(targetStatus);
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(now);

        return mapToSaleResponse(sale);
    }

    /**
     * Actualiza el estado del pago asegurando coherencia con el canal y la logística.
     */
    @Transactional
    public SaleResponse updatePaymentStatus(UUID saleId, PaymentStatusUpdateRequest request, UUID operatorId) {
        Sale            sale                    = this.findSaleByIdOrThrow(saleId);
        PaymentStatus   currentPaymentStatus    = sale.getPaymentStatus();
        PaymentStatus   targetPaymentStatus     = request.newPaymentStatus();

        if (currentPaymentStatus == targetPaymentStatus) return mapToSaleResponse(sale);

        // Validar reglas de transición de pagos
        validatePaymentTransition(sale, currentPaymentStatus, targetPaymentStatus);

        // Aplicar cambio de pago
        sale.setPaymentStatus(targetPaymentStatus);
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(Instant.now());

        // Efectos colaterales automáticos sobre la venta
        if (
            targetPaymentStatus == PaymentStatus.APROBADO &&
            (sale.getPaymentMethod() == PaymentMethod.TRANSFERENCIA && sale.getSaleStatus() == SaleStatus.PENDIENTE)
        ) {
            // Transferencia aprobada avanza automáticamente a PREPARACION
            sale.setSaleStatus(SaleStatus.PREPARACION);
        } else if (
            targetPaymentStatus == PaymentStatus.RECHAZADO &&
            sale.getSaleStatus() == SaleStatus.PENDIENTE
        ) {
            // Si el pago de una orden pendiente es rechazado, se cancela la orden y libera stock
            this.restoreVariantStock(sale.getItems());
            sale.setSaleStatus(SaleStatus.CANCELADO);
        } else if (targetPaymentStatus == PaymentStatus.REEMBOLSADO) {
            sale.setSaleStatus(SaleStatus.REEMBOLSADO);
        }

        // Notas de auditoría interna
        if (request.paymentNotes() != null && !request.paymentNotes().isBlank()) {
            String currentNotes = sale.getInternalNotes() != null ? sale.getInternalNotes() + "\n" : "";
            sale.setInternalNotes(currentNotes + "[Pago " + targetPaymentStatus + "]: " + request.paymentNotes().trim());
        }

        return mapToSaleResponse(sale);
    }

    /**
     * Actualiza o agrega notas internas administrativas sin alterar la máquina de estados.
     */
    @Transactional
    public SaleResponse updateInternalNotes(UUID saleId, SaleInternalNoteRequest request, UUID operatorId) {
        Sale sale = this.findSaleByIdOrThrow(saleId);

        sale.setInternalNotes(request.internalNotes().trim());
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(Instant.now());

        return mapToSaleResponse(sale);
    }

    // =========================================================================
    // Lógica Privada de Soporte
    // =========================================================================

    private void validateSaleStatusCrossRules(Sale sale, SaleStatus targetSaleStatus) {
        PaymentMethod method    = sale.getPaymentMethod();
        PaymentStatus payment   = sale.getPaymentStatus();

        // Regla: No despachar sin cobrar (salvo efectivo)
        if ((targetSaleStatus == SaleStatus.ENVIADO || targetSaleStatus == SaleStatus.ENTREGADO)
                && method != PaymentMethod.EFECTIVO
                && payment != PaymentStatus.APROBADO) {
            throw new InvalidSaleStatusTransitionException(
                    String.format("No se puede pasar a '%s' un pedido con método '%s' cuyo pago está en '%s'. Debe estar APROBADO.",
                            targetSaleStatus, method, payment)
            );
        }

        // Regla: No preparar pedidos de transferencia o MP sin estar aprobados
        if (targetSaleStatus == SaleStatus.PREPARACION
                && method != PaymentMethod.EFECTIVO
                && payment != PaymentStatus.APROBADO) {
            throw new InvalidSaleStatusTransitionException(
                    String.format("No se puede iniciar PREPARACION para pedidos por '%s' hasta que el pago figure APROBADO.", method)
            );
        }

        // Regla: Reembolso logístico exige pago previamente reembolsado o aprobado
        if (targetSaleStatus == SaleStatus.REEMBOLSADO
                && payment != PaymentStatus.APROBADO
                && payment != PaymentStatus.REEMBOLSADO) {
            throw new InvalidSaleStatusTransitionException(
                    "No se puede reembolsar una venta cuyo pago nunca fue APROBADO."
            );
        }
    }

    private void validatePaymentTransition(Sale sale, PaymentStatus current, PaymentStatus target) {
        PaymentMethod method = sale.getPaymentMethod();

        // 1. Protección de pasarela: Mercado Pago solo admite REEMBOLSADO manual por admin
        if (method == PaymentMethod.MERCADO_PAGO && target != PaymentStatus.REEMBOLSADO) {
            throw new InvalidSaleStatusTransitionException(
                    "Los pagos con Mercado Pago son conciliados automáticamente por Webhook. Solo se permite registrar REEMBOLSADO de forma manual."
            );
        }

        // 2. No se puede reembolsar lo no cobrado
        if (target == PaymentStatus.REEMBOLSADO && current != PaymentStatus.APROBADO) {
            throw new InvalidSaleStatusTransitionException(
                    "Solo se puede pasar a REEMBOLSADO un pago que se encuentra APROBADO."
            );
        }

        // 3. Estados terminales de pago
        if (current == PaymentStatus.REEMBOLSADO) {
            throw new InvalidSaleStatusTransitionException("Un pago en estado REEMBOLSADO no puede cambiar de estado.");
        }

        // 4. Pago aprobado no vuelve a pendiente
        if (current == PaymentStatus.APROBADO && target == PaymentStatus.PENDIENTE) {
            throw new InvalidSaleStatusTransitionException("Un pago APROBADO no puede revertirse a PENDIENTE.");
        }
    }

    private void validateTransition(SaleStatus current, SaleStatus target) {
        Set<SaleStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target))
            throw new InvalidSaleStatusTransitionException(current.name(), target.name());
    }

    /**
     * Restituye atómicamente el stock de las variantes correspondientes a una venta cancelada.
     */
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

    private Sale findSaleByIdOrThrow(UUID saleId) {
        return this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("{ ID: %s }".formatted(saleId)));
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

    private SaleResponse mapToSaleResponse(Sale sale) {
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
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}