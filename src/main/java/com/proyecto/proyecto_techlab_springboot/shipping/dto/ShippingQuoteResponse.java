package com.proyecto.proyecto_techlab_springboot.shipping.dto;

import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingMethod;

import java.math.BigDecimal;

public record ShippingQuoteResponse(
        ShippingMethod shippingMethod,
        String zoneName,
        String description,
        BigDecimal shippingCost,
        String estimatedDays,
        boolean cashPaymentAllowed
) {}
