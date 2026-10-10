package com.proyecto.proyecto_techlab_springboot.exception;

public class ColorNotFoundException extends ResourceNotFoundException {
    public ColorNotFoundException(String params) {
        super("color", params);
    }
}
