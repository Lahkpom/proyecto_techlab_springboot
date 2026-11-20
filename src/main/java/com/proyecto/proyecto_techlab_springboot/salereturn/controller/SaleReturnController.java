package com.proyecto.proyecto_techlab_springboot.salereturn.controller;

import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnCreateRequest;
import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnResponse;
import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.salereturn.service.SaleReturnService;
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
 * Rutas de posventa orientadas al cliente final:
 *  - Ejercicio del Botón de Arrepentimiento / Devolución (registrado o invitado)
 *  - Consulta pública por Tracking Token de devolución
 *  - Historial de reclamos propios
 */
@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    /**
     * Inicia una solicitud de devolución para un usuario registrado sobre una orden propia.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/orders/{saleId}")
    public ResponseEntity<SaleReturnResponse> createReturnForCustomer(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleReturnCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                this.saleReturnService.createReturnForUser(saleId, request, principal.userId())
        );
    }

    /**
     * Inicia una solicitud de devolución para compras de invitados mediante el Magic Link de la orden.
     */
    @PostMapping("/guest/{trackingToken}")
    public ResponseEntity<SaleReturnResponse> createReturnForGuest(
            @PathVariable UUID trackingToken,
            @Valid @RequestBody SaleReturnCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.saleReturnService.createReturnByTrackingToken(trackingToken, request));
    }

    /**
     * Consulta pública del expediente de devolución mediante su propio trackingToken.
     */
    @GetMapping("/track/{returnTrackingToken}")
    public ResponseEntity<SaleReturnResponse> trackReturn(@PathVariable UUID returnTrackingToken) {
        return ResponseEntity.ok(this.saleReturnService.getReturnByTrackingToken(returnTrackingToken));
    }

    /**
     * Historial paginado de devoluciones del cliente autenticado.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<Page<SaleReturnSummaryResponse>> getMyReturns(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(this.saleReturnService.getMyReturns(principal.userId(), pageable));
    }
}