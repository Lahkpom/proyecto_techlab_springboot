package com.proyecto.proyecto_techlab_springboot.sale.exception;

public class MissingTaxIdentificationException extends RuntimeException {
    public MissingTaxIdentificationException(String message) {
        super(message);
    }
}