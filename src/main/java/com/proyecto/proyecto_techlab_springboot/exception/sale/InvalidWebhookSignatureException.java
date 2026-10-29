package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}