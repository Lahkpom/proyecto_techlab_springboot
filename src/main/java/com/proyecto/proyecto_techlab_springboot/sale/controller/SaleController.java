package com.proyecto.proyecto_techlab_springboot.sale.controller;

import com.proyecto.proyecto_techlab_springboot.sale.dto.CheckoutRequest;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleResponse;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleService;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Rutas orientadas al cliente
 *  - Checkout unificado anónimo/registrado
 *  - Consulta por Magic Link 
 *  - Consulta de pedidos propios
 */
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    /**
     * Inicia una orden de compra.
     * Permite compras de invitados (sin Authorization) o de usuarios autenticados.
     */
    @PostMapping("/checkout")
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UUID            authenticatedUserId = principal != null ? principal.userId() : null;
        SaleResponse    response            = saleService.createSale(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Permite reintentar el pago con Mercado Pago para una orden propia en estado PENDIENTE.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/{saleId}/retry-mp")
    public ResponseEntity<SaleResponse> retryMercadoPagoPayment(
            @PathVariable UUID saleId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.retryMercadoPagoPayment(saleId, principal.userId()));
    }

    /**
     * Consulta pública segura del estado del pedido mediante token criptográfico (Magic Link).
     */
    @GetMapping("/track/{trackingToken}")
    public ResponseEntity<SaleResponse> trackOrder(@PathVariable UUID trackingToken) {
        return ResponseEntity.ok(saleService.getSaleByTrackingToken(trackingToken));
    }

    /**
     * Historial paginado de compras del cliente logueado.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<Page<SaleSummaryResponse>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.getMyOrders(principal.userId(), pageable));
    }

    /**
     * Consulta una orden específica para el usuario logueado.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/{saleId}")
    public ResponseEntity<SaleResponse> getMyOrderDetail(
            @PathVariable UUID saleId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.getSaleByIdForUser(saleId, principal.userId()));
    }

    /**
     * Cancela una orden propia del cliente si aún está en estado PENDIENTE|PREPARACION.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/{saleId}/cancel")
    public ResponseEntity<SaleResponse> cancelMyOrder(
            @PathVariable UUID saleId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(saleService.cancelMySale(saleId, principal.userId()));
    }
}