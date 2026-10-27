package com.proyecto.proyecto_techlab_springboot.dto.sale;

import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentMethod;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.SaleStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta completa de la orden. Se usa tanto en la pantalla de éxito inmediata del front, como 
 * en el endpoint del Magic Link para invitados y en el historial de compras del usuario.
 */
@Builder
public record SaleResponse(
        UUID                    saleId,
        UUID                    userId,
        UUID                    trackingToken,
        String                  customerName,
        String                  customerEmail,
        String                  customerPhone,
        String                  customerNotes,
        String                  shippingAddress,
        Integer                 saleQuantity,
        BigDecimal              saleSubtotal,
        BigDecimal              saleDiscountAmount,
        BigDecimal              shippingCost,
        BigDecimal              saleTotal,
        Instant                 saleShippedAt,
        Instant                 saleDeliveredAt,
        SaleStatus              saleStatus,
        PaymentMethod           paymentMethod,
        PaymentStatus           paymentStatus,
        String                  internalNotes,
        String                  mpPreferenceId,
        String                  mpInitPoint, // URL provista por MP para redirección al Checkout Pro
        Instant                 createdAt,
        Instant                 updatedAt,
        List<SaleItemResponse>  items
) {}