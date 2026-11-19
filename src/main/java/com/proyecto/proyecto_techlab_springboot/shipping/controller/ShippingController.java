package com.proyecto.proyecto_techlab_springboot.shipping.controller;

import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteRequest;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingQuoteResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.dto.ShippingZoneResponse;
import com.proyecto.proyecto_techlab_springboot.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/options")
    public ResponseEntity<List<ShippingZoneResponse>> getShippingOptions() {
        return ResponseEntity.ok(this.shippingService.getActiveShippingOptions());
    }

    @GetMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quoteShipping(
            @RequestParam boolean isPickup,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String postalCode
    ) {
        ShippingQuoteRequest request = new ShippingQuoteRequest(isPickup, province, locality, postalCode);
        return ResponseEntity.ok(this.shippingService.quoteShipping(request));
    }
}
