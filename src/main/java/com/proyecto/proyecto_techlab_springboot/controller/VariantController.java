package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantResponse;
import com.proyecto.proyecto_techlab_springboot.dto.variant.VariantUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/variantes")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @GetMapping("/producto/{productId}")
    public ResponseEntity<List<VariantResponse>> getVariantsByProduct(
            @PathVariable                           UUID    productId,
            @RequestParam(defaultValue = "false")   boolean includeInactive
    ) {
        return ResponseEntity.ok(variantService.getVariantsByProduct(productId, includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VariantResponse> getVariantById(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.getVariantById(id));
    }

    @PostMapping
    public ResponseEntity<VariantResponse> createVariant(@Valid @RequestBody VariantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.createVariant(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable       UUID                    id,
            @Valid @RequestBody VariantUpdateRequest    request
    ) {
        return ResponseEntity.ok(variantService.updateVariant(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VariantResponse> updateVariantStatus(
            @PathVariable       UUID                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(variantService.updateVariantStatus(id, request));
    }
}