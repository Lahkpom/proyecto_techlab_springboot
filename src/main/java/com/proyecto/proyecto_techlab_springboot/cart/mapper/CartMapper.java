package com.proyecto.proyecto_techlab_springboot.cart.mapper;

import com.proyecto.proyecto_techlab_springboot.cart.dto.CartItemResponse;
import com.proyecto.proyecto_techlab_springboot.cart.dto.CartResponse;
import com.proyecto.proyecto_techlab_springboot.variant.model.Variant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CartMapper {

    public CartItemResponse toItemResponse(
            Variant variant,
            String primaryImage,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            boolean wasAdjusted
    ) {
        return new CartItemResponse(
                variant.getVariantId(),
                variant.getProduct().getProductName(),
                variant.getVariantSku(),
                variant.getVariantDesc(),
                primaryImage,
                unitPrice,
                quantity,
                subtotal,
                variant.getVariantStock(),
                wasAdjusted
        );
    }

    public CartResponse toResponse(
            UUID userId,
            List<CartItemResponse> items,
            int totalUnits,
            BigDecimal grandTotal,
            boolean itemsPurged,
            boolean itemsAdjusted
    ) {
        return new CartResponse(userId, items, totalUnits, grandTotal, itemsPurged, itemsAdjusted);
    }
}
