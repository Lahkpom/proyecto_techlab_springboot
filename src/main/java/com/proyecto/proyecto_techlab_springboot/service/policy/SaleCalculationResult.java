package com.proyecto.proyecto_techlab_springboot.service.policy;

import java.math.BigDecimal;

public record SaleCalculationResult(
        int         totalQuantity,
        BigDecimal  subtotal,
        BigDecimal  discountPercentage,
        BigDecimal  discountAmount,
        BigDecimal  shippingCost,
        BigDecimal  total
) {}