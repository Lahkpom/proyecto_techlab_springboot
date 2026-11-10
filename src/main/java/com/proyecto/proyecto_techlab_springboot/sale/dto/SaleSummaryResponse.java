package com.proyecto.proyecto_techlab_springboot.sale.dto;

import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;
import com.proyecto.proyecto_techlab_springboot.user.model.TaxIdType;
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
        TaxIdType       customerTaxIdType,
        String          customerTaxIdNumber,
        Integer         saleQuantity,
        BigDecimal      saleTotal,
        SaleStatus saleStatus,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        Instant         createdAt
) {}