package com.proyecto.proyecto_techlab_springboot.common.exception.sale;

public class MissingTaxIdentificationException extends RuntimeException {
    public MissingTaxIdentificationException(String message) {
        super(message);
    }
}