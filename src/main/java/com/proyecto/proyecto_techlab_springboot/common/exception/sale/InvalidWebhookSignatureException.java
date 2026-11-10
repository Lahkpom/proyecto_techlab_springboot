package com.proyecto.proyecto_techlab_springboot.common.exception.sale;

public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}