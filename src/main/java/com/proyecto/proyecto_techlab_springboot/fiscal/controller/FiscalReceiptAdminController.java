package com.proyecto.proyecto_techlab_springboot.fiscal.controller;

import com.proyecto.proyecto_techlab_springboot.fiscal.dto.FiscalReceiptResponse;
import com.proyecto.proyecto_techlab_springboot.fiscal.service.FiscalReceiptAdminService;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/fiscal")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class FiscalReceiptAdminController {

    private final FiscalReceiptAdminService fiscalReceiptAdminService;

    @GetMapping("/sales/{saleId}")
    public ResponseEntity<List<FiscalReceiptResponse>> getReceiptsBySale(@PathVariable UUID saleId) {
        return ResponseEntity.ok(this.fiscalReceiptAdminService.getReceiptsBySale(saleId));
    }

    @GetMapping("/{fiscalReceiptId}")
    public ResponseEntity<FiscalReceiptResponse> getReceiptDetail(@PathVariable UUID fiscalReceiptId) {
        return ResponseEntity.ok(this.fiscalReceiptAdminService.getReceiptDetail(fiscalReceiptId));
    }

    @PostMapping("/sales/{saleId}/retry-invoice")
    public ResponseEntity<FiscalReceiptResponse> retryRejectedInvoiceForSale(
            @PathVariable UUID saleId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(this.fiscalReceiptAdminService.retryRejectedInvoiceForSale(saleId, principal.userId()));
    }
}