package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.sale.CheckoutRequest;
import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleResponse;
import com.proyecto.proyecto_techlab_springboot.dto.sale.SaleSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.service.SaleService;
import com.proyecto.proyecto_techlab_springboot.util.SecurityContextUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final SaleService           saleService;
    private final SecurityContextUtil   securityContextUtil;

    /**
     * Inicia una orden de compra.
     * Permite compras de invitados (sin Authorization) o de usuarios autenticados.
     */
    @PostMapping("/checkout")
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID            authenticatedUserId = this.securityContextUtil.resolveOptionalUserIdFromToken(httpRequest);
        SaleResponse    response            = saleService.createSale(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    // @PreAuthorize("isAuthenticated()") // TODO: Descomentar al habilitar Spring Security
    @GetMapping("/me")
    public ResponseEntity<Page<SaleSummaryResponse>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.ok(saleService.getMyOrders(userId, pageable));
    }

    /**
     * Consulta una orden específica para el usuario logueado.
     */
    // @PreAuthorize("isAuthenticated()") // TODO: Descomentar al habilitar Spring Security
    @GetMapping("/me/{saleId}")
    public ResponseEntity<SaleResponse> getMyOrderDetail(
            @PathVariable UUID saleId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.ok(saleService.getSaleByIdForUser(saleId, userId));
    }
}