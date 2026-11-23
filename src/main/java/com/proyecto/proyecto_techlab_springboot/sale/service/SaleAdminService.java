package com.proyecto.proyecto_techlab_springboot.sale.service;

import com.proyecto.proyecto_techlab_springboot.sale.exception.InvalidSaleStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.sale.exception.SaleNotFoundException;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.sale.repository.ISaleRepository;
import com.proyecto.proyecto_techlab_springboot.sale.dto.*;
import com.proyecto.proyecto_techlab_springboot.sale.mapper.SaleMapper;
import com.proyecto.proyecto_techlab_springboot.fiscal.service.FiscalReceiptService;
import com.proyecto.proyecto_techlab_springboot.user.exception.InvalidUserDataException;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
@Slf4j
public class SaleAdminService {

    private final ISaleRepository       iSaleRepository;
    private final SaleMapper            saleMapper;
    private final VariantService variantService;
    private final FiscalReceiptService  fiscalReceiptService;
    private final UserService userService;

    // Matriz de transiciones de ciclo de vida legales
    private static final Map<SaleStatus, Set<SaleStatus>> ALLOWED_TRANSITIONS = Map.of(
            SaleStatus.PENDIENTE    , Set.of(SaleStatus.PREPARACION , SaleStatus.CANCELADO, SaleStatus.AUDITORIA),
            SaleStatus.AUDITORIA    , Set.of(SaleStatus.PREPARACION , SaleStatus.CANCELADO),
            SaleStatus.PREPARACION  , Set.of(SaleStatus.ENVIADO     , SaleStatus.CANCELADO),
            SaleStatus.ENVIADO      , Set.of(SaleStatus.ENTREGADO   , SaleStatus.CANCELADO),
            SaleStatus.ENTREGADO    , Set.of(SaleStatus.REEMBOLSADO),
            SaleStatus.CANCELADO    , Set.of(), // Estado terminal
            SaleStatus.REEMBOLSADO  , Set.of()  // Estado terminal
    );

    /**
     * Búsqueda paginada multicriterio para el panel administrativo.
     */
    @Transactional(readOnly = true)
    public Page<SaleSummaryResponse> searchSales(
            boolean         auditOnly,
            List<SaleStatus> statuses,
            SaleStatus      saleStatus,
            PaymentStatus   paymentStatus,
            Boolean         fiscalRejection,
            String          customerEmail,
            Instant         startDate,
            Instant         endDate,
            Pageable        pageable
    ) {
        boolean filterStatuses = statuses != null && !statuses.isEmpty();
        List<SaleStatus> effectiveStatuses = filterStatuses ? statuses : List.of(SaleStatus.values());
        String customerEmailPattern = (customerEmail == null || customerEmail.isBlank())
                ? "%%"
                : "%" + customerEmail.trim() + "%";
        return this.iSaleRepository.searchSalesAdmin(
                auditOnly,
                SaleStatus.AUDITORIA,
                List.of(
                        SaleStatus.ENTREGADO,
                        SaleStatus.CANCELADO,
                        SaleStatus.REEMBOLSADO,
                        SaleStatus.CERRADO_CON_DEVOLUCION
                ),
                filterStatuses,
                effectiveStatuses,
                saleStatus != null,
                saleStatus,
                paymentStatus != null,
                paymentStatus,
                fiscalRejection != null,
                fiscalRejection,
                customerEmailPattern,
                startDate != null,
                startDate,
                endDate != null,
                endDate,
                pageable
        ).map(saleMapper::toSummaryResponse);
    }

    /**
     * Obtiene el detalle completo de cualquier orden por su ID.
     */
    @Transactional(readOnly = true)
    public SaleResponse getSaleDetail(UUID saleId) {
        return saleMapper.toResponse(this.findSaleByIdOrThrow(saleId), null);
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

        if (currentStatus == targetStatus) return saleMapper.toResponse(sale, null);

        // Validar máquina de estados logísticos
        this.validateTransition(currentStatus, targetStatus);

        // Validaciones cruzadas de negocio
        validateSaleStatusCrossRules(sale, targetStatus);

        // Manejo de stock ante cancelación
        if (targetStatus == SaleStatus.CANCELADO && currentStatus != SaleStatus.ENTREGADO)
            this.variantService.restoreStockForSaleItems(sale.getItems());

        // Efectivo entregado aprueba el cobro y dispara Factura C
        if (targetStatus == SaleStatus.ENTREGADO && sale.getPaymentMethod() == PaymentMethod.EFECTIVO) {
            sale.setPaymentStatus(PaymentStatus.APROBADO);
            this.fiscalReceiptService.tryEmitInvoiceSafely(sale.getSaleId(), operatorId);
        }

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

        log.info("Transición manual de venta: saleId={}, estadoAnterior={}, estadoNuevo={}, operatorId={}",
                saleId, currentStatus, targetStatus, operatorId);
        return saleMapper.toResponse(sale, null);
    }

    /**
     * Actualiza el estado del pago asegurando coherencia con el canal y la logística.
     */
    @Transactional
    public SaleResponse updatePaymentStatus(UUID saleId, PaymentStatusUpdateRequest request, UUID operatorId) {
        Sale            sale                    = this.findSaleByIdOrThrow(saleId);
        PaymentStatus   currentPaymentStatus    = sale.getPaymentStatus();
        PaymentStatus   targetPaymentStatus     = request.newPaymentStatus();
        SaleStatus      currentSaleStatus       = sale.getSaleStatus();

        if (currentPaymentStatus == targetPaymentStatus) return saleMapper.toResponse(sale, null);

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
            this.fiscalReceiptService.tryEmitInvoiceSafely(sale.getSaleId(), operatorId);
        } else if (
            targetPaymentStatus == PaymentStatus.RECHAZADO &&
            sale.getSaleStatus() == SaleStatus.PENDIENTE
        ) {
            // Si el pago de una orden pendiente es rechazado, se cancela la orden y libera stock
            this.variantService.restoreStockForSaleItems(sale.getItems());
            sale.setSaleStatus(SaleStatus.CANCELADO);
        } else if (targetPaymentStatus == PaymentStatus.REEMBOLSADO) {
            sale.setSaleStatus(SaleStatus.REEMBOLSADO);
        }

        // Notas de auditoría interna
        if (request.paymentNotes() != null && !request.paymentNotes().isBlank()) {
            String operatorName = this.userService.getUserById(operatorId).userName();
            sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                    sale.getInternalNotes(),
                    operatorName + " - Pago " + targetPaymentStatus,
                    request.paymentNotes().trim()
            ));
        }

        log.info(
                "Conciliación manual de pago: saleId={}, estadoPagoAnterior={}, estadoPagoNuevo={}, estadoVentaAnterior={}, estadoVentaNuevo={}, operatorId={}",
                saleId, currentPaymentStatus, targetPaymentStatus, currentSaleStatus, sale.getSaleStatus(), operatorId
        );
        return saleMapper.toResponse(sale, null);
    }

    /**
     * Actualiza o agrega notas internas administrativas sin alterar la máquina de estados.
     */
    @Transactional
    public SaleResponse updateInternalNotes(UUID saleId, SaleInternalNoteRequest request, UUID operatorId) {
        Sale sale = this.findSaleByIdOrThrow(saleId);

        String operatorName = this.userService.getUserById(operatorId).userName();
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                operatorName,
                request.internalNotes().trim()
        ));
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(Instant.now());

        return saleMapper.toResponse(sale, null);
    }

    @Transactional
    public SaleResponse updateSaleFiscalData(UUID saleId, SaleFiscalDataUpdateRequest request, UUID operatorId) {
        Sale sale = this.findSaleByIdOrThrow(saleId);
        if (!sale.isFiscalRejection()) {
            throw new InvalidSaleStatusTransitionException(
                    "Solo se pueden corregir datos fiscales de ventas con rechazo en ARCA."
            );
        }

        TaxIdType normalizedType = request.customerTaxIdType();
        String normalizedNumber = normalizeTaxIdNumber(normalizedType, request.customerTaxIdNumber());
        if ((normalizedType != null && normalizedNumber == null)
                || (normalizedType == null && normalizedNumber != null)) {
            throw new InvalidUserDataException(
                    "Debe completar tanto el tipo como el número de documento fiscal."
            );
        }

        boolean taxDataChanged = !Objects.equals(sale.getCustomerTaxIdType(), normalizedType)
                || !Objects.equals(sale.getCustomerTaxIdNumber(), normalizedNumber);

        sale.setCustomerName(request.customerName().trim());
        sale.setCustomerTaxIdType(normalizedType);
        sale.setCustomerTaxIdNumber(normalizedNumber);

        String operatorName = this.userService.getUserById(operatorId).userName();
        sale.setInternalNotes(SaleInternalNoteFormatter.prepend(
                sale.getInternalNotes(),
                operatorName + " - Datos Fiscales",
                "Actualizó los datos fiscales del receptor."
        ));
        sale.setUpdatedBy(operatorId);
        sale.setUpdatedAt(Instant.now());

        if (sale.getUser() != null && taxDataChanged) {
            this.userService.updateUserTaxData(sale.getUser().getUserId(), normalizedType, normalizedNumber);
        }

        return saleMapper.toResponse(sale, null);
    }

    private String normalizeTaxIdNumber(TaxIdType taxIdType, String taxIdNumber) {
        if (taxIdNumber == null || taxIdNumber.isBlank()) {
            return null;
        }
        String normalized = taxIdType == TaxIdType.PASAPORTE
                ? taxIdNumber.replaceAll("[^A-Za-z0-9]", "")
                : taxIdNumber.replaceAll("\\D", "");
        return normalized.isEmpty() ? null : normalized;
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
                && payment != PaymentStatus.APROBADO
        ) {
            throw new InvalidSaleStatusTransitionException(
                    "No se puede pasar a '%s' un pedido con método '%s' cuyo pago está en '%s'. Debe estar APROBADO."
                            .formatted(targetSaleStatus, method, payment)
            );
        }

        // Regla: No preparar pedidos de transferencia o MP sin estar aprobados
        if (targetSaleStatus == SaleStatus.PREPARACION
                && method != PaymentMethod.EFECTIVO
                && payment != PaymentStatus.APROBADO
        ) {
            throw new InvalidSaleStatusTransitionException(
                    "No se puede iniciar PREPARACION para pedidos por '%s' hasta que el pago figure APROBADO.".formatted(method)
            );
        }

        // Regla: Reembolso logístico exige pago previamente reembolsado o aprobado
        if (targetSaleStatus == SaleStatus.REEMBOLSADO
                && payment != PaymentStatus.APROBADO
                && payment != PaymentStatus.REEMBOLSADO
        ) {
            throw new InvalidSaleStatusTransitionException(
                    "No se puede reembolsar una venta cuyo pago nunca fue APROBADO."
            );
        }
    }

    private void validatePaymentTransition(Sale sale, PaymentStatus current, PaymentStatus target) {
        PaymentMethod method = sale.getPaymentMethod();

        // Protección de pasarela: Mercado Pago solo admite REEMBOLSADO manual por operador
        if (method == PaymentMethod.MERCADO_PAGO && target != PaymentStatus.REEMBOLSADO)
            throw new InvalidSaleStatusTransitionException(
                    "Los pagos con Mercado Pago son conciliados automáticamente por Webhook. Solo se permite registrar REEMBOLSADO de forma manual."
            );

        // No se puede reembolsar lo no cobrado
        if (target == PaymentStatus.REEMBOLSADO && current != PaymentStatus.APROBADO)
            throw new InvalidSaleStatusTransitionException(
                    "Solo se puede pasar a REEMBOLSADO un pago que se encuentra APROBADO."
            );

        // Estados terminales de pago
        if (current == PaymentStatus.REEMBOLSADO)
            throw new InvalidSaleStatusTransitionException("Un pago en estado REEMBOLSADO no puede cambiar de estado.");

        // Pago aprobado no vuelve a pendiente
        if (current == PaymentStatus.APROBADO && target == PaymentStatus.PENDIENTE)
            throw new InvalidSaleStatusTransitionException("Un pago APROBADO no puede revertirse a PENDIENTE.");
    }

    private void validateTransition(SaleStatus current, SaleStatus target) {
        Set<SaleStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target))
            throw new InvalidSaleStatusTransitionException(current.name(), target.name());
    }

    private Sale findSaleByIdOrThrow(UUID saleId) {
        return this.iSaleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException("{ ID: %s }".formatted(saleId)));
    }

}