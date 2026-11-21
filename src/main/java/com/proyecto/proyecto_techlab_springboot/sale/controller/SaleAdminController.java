package com.proyecto.proyecto_techlab_springboot.sale.controller;

import com.proyecto.proyecto_techlab_springboot.sale.dto.PaymentStatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleInternalNoteRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleFiscalDataUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleResponse;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleStatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleAdminService;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rutas de gestión operativa y logística para administradores sobre las ventas.
 *  - Búsqueda multicriterio paginada, detalle, cambio de estado logístico y notas internas.
 */
@RestController
@RequestMapping("/admin/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class SaleAdminController {

    private final SaleAdminService saleAdminService;

    /**
     * Búsqueda paginada multicriterio de órdenes de compra.
     */
    @GetMapping
    public ResponseEntity<Page<SaleSummaryResponse>> searchSales(
            @RequestParam(defaultValue = "false") boolean auditOnly,
            @RequestParam(required = false) List<SaleStatus> statuses,
            @RequestParam(required = false) SaleStatus      saleStatus,
            @RequestParam(required = false) PaymentStatus   paymentStatus,
            @RequestParam(required = false) Boolean         fiscalRejection,
            @RequestParam(required = false) String          customerEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20)     Pageable pageable
    ) {
        Page<SaleSummaryResponse> sales = saleAdminService.searchSales(
                auditOnly,
                statuses,
                saleStatus,
                paymentStatus,
                fiscalRejection,
                customerEmail,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(sales);
    }

    /**
     * Obtiene el detalle completo y renglones de una orden específica.
     */
    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponse> getSaleDetail(@PathVariable UUID saleId) {
        return ResponseEntity.ok(saleAdminService.getSaleDetail(saleId));
    }

    /**
     * Modifica el estado logístico de la orden respetando la máquina de estados.
     */
    @PatchMapping("/{saleId}/status")
    public ResponseEntity<SaleResponse> updateSaleStatus(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleStatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleAdminService.updateSaleStatus(saleId, request, principal.userId()));
    }

    /**
     * Conciliación manual del estado del pago por parte de un administrador.
     */
    @PatchMapping("/{saleId}/payment-status")
    public ResponseEntity<SaleResponse> updatePaymentStatus(
            @PathVariable UUID saleId,
            @Valid @RequestBody PaymentStatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleAdminService.updatePaymentStatus(saleId, request, principal.userId()));
    }

    /**
     * Agrega o edita las notas internas operativas de una orden.
     */
    @PatchMapping("/{saleId}/internal-notes")
    public ResponseEntity<SaleResponse> updateInternalNotes(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleInternalNoteRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleAdminService.updateInternalNotes(saleId, request, principal.userId()));
    }

    @PatchMapping("/{saleId}/fiscal-data")
    public ResponseEntity<SaleResponse> updateSaleFiscalData(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleFiscalDataUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleAdminService.updateSaleFiscalData(saleId, request, principal.userId()));
    }
}