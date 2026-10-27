package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.service.policy.SaleCalculationResult;
import com.proyecto.proyecto_techlab_springboot.exception.sale.MinimumWholesalerQuantityException;
import com.proyecto.proyecto_techlab_springboot.model.enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SalePolicyService {

    private static final int WHOLESALER_MINIMUM_QUANTITY    = 10;
    private static final int CUSTOMER_DISCOUNT_THRESHOLD    = 3;

    private static final BigDecimal DISCOUNT_CUSTOMER_TIER  = new BigDecimal("5.00");
    private static final BigDecimal DISCOUNT_WHOLESALER     = new BigDecimal("10.00");
    private static final BigDecimal DISCOUNT_ADMIN          = new BigDecimal("10.00");
    private static final BigDecimal DISCOUNT_SUPERADMIN     = new BigDecimal("15.00");
    private static final BigDecimal ONE_HUNDRED             = new BigDecimal("100.00");

    /**
     * Valida reglas mínimas de compra y calcula subtotal, descuentos y total final de la orden.
     *
     * @param totalQuantity Cantidad acumulada de prendas en la orden
     * @param subtotal      Suma de (precio_unitario * cantidad) de todas las variantes
     * @param shippingCost  Costo de envío cotizado
     * @param buyerRole     Rol del comprador (o null si es compra anónima / invitado)
     * @return SaleCalculationResult con los montos inmutables calculados
     */
    public SaleCalculationResult calculateSaleTotals(
            int         totalQuantity,
            BigDecimal  subtotal,
            BigDecimal  shippingCost,
            UserRole    buyerRole
    ) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El subtotal debe ser un monto mayor a cero.");

        BigDecimal safeShippingCost = (shippingCost != null && shippingCost.compareTo(BigDecimal.ZERO) >= 0)
                ? shippingCost.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal safeSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // Validación de compra mínima mayorista
        if (buyerRole == UserRole.WHOLESALER && totalQuantity < WHOLESALER_MINIMUM_QUANTITY)
            throw new MinimumWholesalerQuantityException(totalQuantity, WHOLESALER_MINIMUM_QUANTITY);

        // Determinar porcentaje de descuento
        BigDecimal discountPercentage = resolveDiscountPercentage(buyerRole, totalQuantity);

        // Calcular monto de descuento en moneda: (subtotal * porcentaje) / 100
        BigDecimal discountAmount = safeSubtotal
                .multiply(discountPercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        // Total = subtotal - descuento + envío
        BigDecimal total = safeSubtotal
                .subtract(discountAmount)
                .add(safeShippingCost)
                .setScale(2, RoundingMode.HALF_UP);

        return new SaleCalculationResult(
                totalQuantity,
                safeSubtotal,
                discountPercentage,
                discountAmount,
                safeShippingCost,
                total
        );
    }

    /**
     * Aplica la política comercial acordada:
     * - CUSTOMER   : 5% a partir de la 3ra unidad inclusive (Incluye a los invitados/null).
     * - WHOLESALER : 10% fijo (requiere validación previa de >= 10 unidades).
     * - ADMIN      : 10% plano sin mínimo.
     * - SUPER_ADMIN: 15% plano sin mínimo.
     */
    public BigDecimal resolveDiscountPercentage(UserRole buyerRole, int totalQuantity) {
        // Invitado (null) opera con las mismas reglas que un CUSTOMER
        UserRole effectiveRole = Objects.requireNonNullElse(buyerRole, UserRole.CUSTOMER);

        return switch (effectiveRole) {
            case SUPER_ADMIN -> DISCOUNT_SUPERADMIN;
            case ADMIN       -> DISCOUNT_ADMIN;
            case WHOLESALER  -> DISCOUNT_WHOLESALER;
            case CUSTOMER    -> (totalQuantity >= CUSTOMER_DISCOUNT_THRESHOLD) 
                ? DISCOUNT_CUSTOMER_TIER 
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }
}