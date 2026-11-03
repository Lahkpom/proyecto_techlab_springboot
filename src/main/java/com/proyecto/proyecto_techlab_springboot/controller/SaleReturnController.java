package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.salereturn.SaleReturnCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.salereturn.SaleReturnResponse;
import com.proyecto.proyecto_techlab_springboot.dto.salereturn.SaleReturnSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.service.SaleReturnService;
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
 * Rutas de posventa orientadas al cliente final:
 *  - Ejercicio del Botón de Arrepentimiento / Devolución (registrado o invitado)
 *  - Consulta pública por Tracking Token de devolución
 *  - Historial de reclamos propios
 */
@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
public class SaleReturnController {

    private final SaleReturnService  saleReturnService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * Inicia una solicitud de devolución para un usuario registrado sobre una orden propia.
     */
    // @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')") // TODO: Descomentar al habilitar Spring Security
    @PostMapping("/orders/{saleId}")
    public ResponseEntity<SaleReturnResponse> createReturnForCustomer(
            @PathVariable UUID saleId,
            @Valid @RequestBody SaleReturnCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID authenticatedUserId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.saleReturnService.createReturnForUser(saleId, request, authenticatedUserId));
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
    // @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')") // TODO: Descomentar al habilitar Spring Security
    @GetMapping("/me")
    public ResponseEntity<Page<SaleReturnSummaryResponse>> getMyReturns(
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        UUID authenticatedUserId = this.securityContextUtil.resolveRequiredUserIdFromToken(httpRequest);
        return ResponseEntity.ok(this.saleReturnService.getMyReturns(authenticatedUserId, pageable));
    }
}