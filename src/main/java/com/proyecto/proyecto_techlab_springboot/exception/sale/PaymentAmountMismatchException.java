package com.proyecto.proyecto_techlab_springboot.exception.sale;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentAmountMismatchException extends RuntimeException {

    public PaymentAmountMismatchException(UUID saleId, BigDecimal expectedAmount, BigDecimal actualAmount) {
        super("Discrepancia en el monto del pago para la orden ID: %s. Esperado: $%s, Recibido de MP: $%s"
                .formatted(saleId, expectedAmount, actualAmount)
        );
    }

    public PaymentAmountMismatchException(String message) {
        super(message);
    }
}