package com.proyecto.proyecto_techlab_springboot.exception;

public class VariantNotFoundException extends ResourceNotFoundException {
    public VariantNotFoundException(String params) {
        super("variante", params);
    }
}
