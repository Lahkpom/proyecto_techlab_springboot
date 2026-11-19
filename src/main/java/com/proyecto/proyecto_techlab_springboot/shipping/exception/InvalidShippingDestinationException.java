package com.proyecto.proyecto_techlab_springboot.shipping.exception;

public class InvalidShippingDestinationException extends RuntimeException {
    public InvalidShippingDestinationException(String message) {
        super(message);
    }
}
