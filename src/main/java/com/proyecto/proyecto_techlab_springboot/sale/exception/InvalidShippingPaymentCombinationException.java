package com.proyecto.proyecto_techlab_springboot.sale.exception;

public class InvalidShippingPaymentCombinationException extends RuntimeException {
    public InvalidShippingPaymentCombinationException(String message) {
        super(message);
    }
}
