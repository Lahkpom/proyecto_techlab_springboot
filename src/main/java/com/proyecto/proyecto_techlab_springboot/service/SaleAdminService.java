package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.dto.sale.*;
import com.proyecto.proyecto_techlab_springboot.exception.sale.InvalidSaleStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.exception.sale.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.model.Sale;
import com.proyecto.proyecto_techlab_springboot.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.model.Variant;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
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
import java.util.function.Function;
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
     * Avanza o cambia el estado logístico de una orden respetando la máquina de estados.
     * Si la orden pasa a CANCELADO, restituye el stock comprometido.
     */
    @Transactional
    public SaleResponse updateSaleStatus(UUID saleId, SaleStatusUpdateRequest request, UUID operatorId) {
        Sale        sale            = this.findSaleByIdOrThrow(saleId);
        SaleStatus  currentStatus   = sale.getSaleStatus();
        SaleStatus  targetStatus    = request.newStatus();

        if (currentStatus == targetStatus) return mapToSaleResponse(sale);

        this.validateTransition(currentStatus, targetStatus);

        // Si se cancela antes de ser entregada, reingresar stock
        if (targetStatus == SaleStatus.CANCELADO && currentStatus != SaleStatus.ENTREGADO)
            this.restoreVariantStock(sale.getItems());

        // Marcas temporales de logística
        Instant now = Instant.now();
        if (targetStatus == SaleStatus.ENVIADO && sale.getSaleShippedAt() == null) {
            sale.setSaleShippedAt(now);
        } else if (targetStatus == SaleStatus.ENTREGADO && sale.getSaleDeliveredAt() == null) {
            sale.setSaleDeliveredAt(now);
        }

        sale.setSaleStatus(targetStatus);
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(now);

        Sale updatedSale = this.iSaleRepository.save(sale);
        return mapToSaleResponse(updatedSale);
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

        Sale updatedSale = this.iSaleRepository.save(sale);
        return mapToSaleResponse(updatedSale);
    }

    // =========================================================================
    // Lógica Privada de Soporte
    // =========================================================================

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