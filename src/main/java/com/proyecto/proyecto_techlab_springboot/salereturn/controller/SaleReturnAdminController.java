package com.proyecto.proyecto_techlab_springboot.salereturn.controller;

import com.proyecto.proyecto_techlab_springboot.salereturn.dto.*;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;
import com.proyecto.proyecto_techlab_springboot.salereturn.service.SaleReturnAdminService;
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
 * Panel de operaciones posventa para administradores e inspectores de taller:
 *  - Auditoría y seguimiento de expedientes
 *  - Transiciones logísticas y asignación de guías
 *  - Peritaje físico de prendas en mesa de control
 *  - Ejecución y conciliación de reembolsos
 */
@RestController
@RequestMapping("/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class SaleReturnAdminController {

    private final SaleReturnAdminService saleReturnAdminService;

    /**
     * Búsqueda paginada multicriterio de devoluciones para el panel administrativo.
     */
    @GetMapping
    public ResponseEntity<Page<SaleReturnSummaryResponse>> searchReturns(
            @RequestParam(required = false) ReturnStatus    returnStatus,
            @RequestParam(required = false) ReturnReason    returnReason,
            @RequestParam(required = false) String          customerEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        return ResponseEntity.ok(this.saleReturnAdminService.searchReturns(
                returnStatus, returnReason, customerEmail, startDate, endDate, pageable
        ));
    }

    @GetMapping("/sales/{saleId}")
    public ResponseEntity<List<SaleReturnResponse>> getReturnsForSale(@PathVariable UUID saleId) {
        return ResponseEntity.ok(this.saleReturnAdminService.getReturnsForSale(saleId));
    }

    /**
     * Recupera el expediente completo de una devolución (cabecera, ítems e inspección).
     */
    @GetMapping("/{returnId}")
    public ResponseEntity<SaleReturnResponse> getReturnDetail(@PathVariable UUID returnId) {
        return ResponseEntity.ok(this.saleReturnAdminService.getReturnDetail(returnId));
    }

    /**
     * Modifica el estado logístico de la devolución (ej: EN_TRANSITO, RECIBIDO_EN_CONTROL, CANCELADA).
     */
    @PatchMapping("/{returnId}/status")
    public ResponseEntity<SaleReturnResponse> updateReturnStatus(
            @PathVariable UUID returnId,
            @Valid @RequestBody SaleReturnStatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                this.saleReturnAdminService.updateReturnStatus(returnId, request, principal.userId())
        );
    }

    /**
     * Asienta el peritaje físico de calidad en taller prenda por prenda.
     */
    @PostMapping("/{returnId}/inspect")
    public ResponseEntity<SaleReturnResponse> inspectReturn(
            @PathVariable UUID returnId,
            @Valid @RequestBody SaleReturnInspectionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                this.saleReturnAdminService.inspectReturn(returnId, request, principal.userId())
        );
    }

    /**
     * Ejecuta el reembolso por pasarela (Mercado Pago) o asienta la conciliación bancaria manual.
     */
    @PostMapping("/{returnId}/process-refund")
    public ResponseEntity<SaleReturnResponse> processRefund(
            @PathVariable UUID returnId,
            @Valid @RequestBody(required = false) SaleReturnRefundRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                this.saleReturnAdminService.processRefund(returnId, request, principal.userId())
        );
    }
}