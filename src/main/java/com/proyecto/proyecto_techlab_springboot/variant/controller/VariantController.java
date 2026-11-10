package com.proyecto.proyecto_techlab_springboot.variant.controller;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantCreateRequest;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantResponse;
import com.proyecto.proyecto_techlab_springboot.variant.dto.VariantUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.variant.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<List<VariantResponse>> getVariantsByProduct(
            @PathVariable                           UUID    productId,
            @RequestParam(defaultValue = "false")   boolean includeInactive
    ) {
        return ResponseEntity.ok(variantService.getVariantsByProduct(productId, includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VariantResponse> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(variantService.getVariantById(id));
    }

    @PostMapping
    //    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<VariantResponse> createVariant(@Valid @RequestBody VariantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.createVariant(request));
    }

    @PutMapping("/{id}")
    //    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable       Long                    id,
            @Valid @RequestBody VariantUpdateRequest    request
    ) {
        return ResponseEntity.ok(variantService.updateVariant(id, request));
    }

    @PatchMapping("/{id}/status")
    //    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<VariantResponse> updateVariantStatus(
            @PathVariable       Long                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(variantService.updateVariantStatus(id, request));
    }
}