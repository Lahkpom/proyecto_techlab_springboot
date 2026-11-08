package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.fiscal.FiscalReceiptResponse;
import com.proyecto.proyecto_techlab_springboot.service.fiscal.FiscalReceiptAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/fiscal")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
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
}