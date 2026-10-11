package com.proyecto.proyecto_techlab_springboot.exception;

public class ColorAlreadyExistsException extends ResourceAlreadyExistsException {
    public ColorAlreadyExistsException(String params) {
        super("color", params);
    }
}
