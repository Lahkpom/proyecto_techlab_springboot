package com.proyecto.proyecto_techlab_springboot.shipping.controller;

import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.shipping.service.ShippingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class ShippingAdminController {

    private final ShippingService shippingService;

    @GetMapping
    public ResponseEntity<List<ShippingZoneResponse>> getShippingZones() {
        return ResponseEntity.ok(shippingService.getShippingZones());
    }

    @GetMapping("/{shippingZoneId}")
    public ResponseEntity<ShippingZoneResponse> getShippingZone(@PathVariable Long shippingZoneId) {
        return ResponseEntity.ok(shippingService.getShippingZone(shippingZoneId));
    }

    @PutMapping("/{shippingZoneId}")
    public ResponseEntity<ShippingZoneResponse> updateShippingZone(
            @PathVariable Long shippingZoneId,
            @Valid @RequestBody ShippingZoneUpdateRequest request
    ) {
        return ResponseEntity.ok(shippingService.updateShippingZone(shippingZoneId, request));
    }

    @PatchMapping("/{shippingZoneId}/status")
    public ResponseEntity<ShippingZoneResponse> updateShippingZoneStatus(
            @PathVariable Long shippingZoneId,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(shippingService.updateShippingZoneStatus(shippingZoneId, request));
    }
}
