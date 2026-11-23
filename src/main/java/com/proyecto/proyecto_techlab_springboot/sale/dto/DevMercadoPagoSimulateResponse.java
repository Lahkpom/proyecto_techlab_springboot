package com.proyecto.proyecto_techlab_springboot.sale.dto;

import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.SaleStatus;

import java.util.UUID;

public record DevMercadoPagoSimulateResponse(
        UUID saleId,
        String redirectUrl,
        SaleStatus saleStatus,
        PaymentStatus paymentStatus
) {}
