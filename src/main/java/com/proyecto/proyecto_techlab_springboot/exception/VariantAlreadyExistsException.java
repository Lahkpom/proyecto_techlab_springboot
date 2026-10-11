package com.proyecto.proyecto_techlab_springboot.exception;

public class VariantAlreadyExistsException extends ResourceAlreadyExistsException {
    public VariantAlreadyExistsException(String params) {
        super("variante", params);
    }
}
