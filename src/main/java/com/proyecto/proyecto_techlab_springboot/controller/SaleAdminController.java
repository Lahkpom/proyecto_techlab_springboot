package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleInternalNoteRequest;
import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleResponse;
import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleStatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.service.SaleAdminService;
import com.proyecto.proyecto_techlab_springboot.util.SecurityContextUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Rutas de gestión operativa y logística para administradores sobre las ventas.
 *  - Búsqueda multicriterio paginada, detalle, cambio de estado logístico y notas internas.
 */
@RestController
@RequestMapping("/admin/sales")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')") // TODO: Descomentar al habilitar Spring Security
public class SaleAdminController {

    private final SaleAdminService      saleAdminService;
    private final SecurityContextUtil   securityContextUtil;

    /**
     * Búsqueda paginada multicriterio de órdenes de compra.
     */
    @GetMapping
    public ResponseEntity<Page<SaleSummaryResponse>> searchSales(
            @RequestParam(required = false) SaleStatus      saleStatus,
            @RequestParam(required = false) PaymentStatus   paymentStatus,
            @RequestParam(required = false) String          customerEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20)     Pageable pageable
    ) {
        Page<SaleSummaryResponse> sales = saleAdminService.searchSales(
                saleStatus,
                paymentStatus,
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
            HttpServletRequest httpRequest
    ) {
        UUID operatorId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.ok(saleAdminService.updateSaleStatus(saleId, request, operatorId));
    }

    /**
     * Agrega o edita las notas internas operativas de una orden.
     */
    @PatchMapping("/{saleId}/internal-notes")
    public ResponseEntity<SaleResponse> updateInternalNotes(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleInternalNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID operatorId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.ok(saleAdminService.updateInternalNotes(saleId, request, operatorId));
    }
}