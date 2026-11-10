package com.proyecto.proyecto_techlab_springboot.common.exception.common;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
