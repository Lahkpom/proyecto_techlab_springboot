package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vista liviana para listados paginados en paneles administrativos o listados de historial de pedidos.
 */
@Builder
public record SaleSummaryResponse(
        UUID            saleId,
        UUID            userId,
        UUID            trackingToken,
        String          customerName,
        String          customerEmail,
        Integer         saleQuantity,
        BigDecimal      saleTotal,
        SaleStatus      saleStatus,
        PaymentMethod   paymentMethod,
        PaymentStatus   paymentStatus,
        Instant         createdAt
) {}