package com.proyecto.proyecto_techlab_springboot.sale.dto;

import java.math.BigDecimal;

public record SaleCalculationResult(
        int         totalQuantity,
        BigDecimal  subtotal,
        BigDecimal  discountPercentage,
        BigDecimal  discountAmount,
        BigDecimal  shippingCost,
        BigDecimal  total
) {}