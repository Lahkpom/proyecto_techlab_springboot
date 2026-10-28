package com.proyecto.proyecto_techlab_springboot.exception.common;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
