package com.proyecto.proyecto_techlab_springboot.sale.exception;

public class MercadoPagoIntegrationException extends RuntimeException {
    public MercadoPagoIntegrationException(String message) {
        super(message);
    }

    public MercadoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}