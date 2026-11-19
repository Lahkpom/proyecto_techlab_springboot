package com.proyecto.proyecto_techlab_springboot.shipping.dto;

import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.shipping.model.ShippingMethod;

import java.math.BigDecimal;
import java.util.List;

public record ShippingZoneResponse(
        Long shippingZoneId,
        ShippingMethod shippingMethod,
        String zoneName,
        String description,
        List<String> postalCodes,
        BigDecimal baseCost,
        String estimatedDays,
        RowStatus rowStatus
) {}
