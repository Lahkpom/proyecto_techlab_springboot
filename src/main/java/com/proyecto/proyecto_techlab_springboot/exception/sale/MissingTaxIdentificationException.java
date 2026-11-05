package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class MissingTaxIdentificationException extends RuntimeException {
    public MissingTaxIdentificationException(String message) {
        super(message);
    }
}