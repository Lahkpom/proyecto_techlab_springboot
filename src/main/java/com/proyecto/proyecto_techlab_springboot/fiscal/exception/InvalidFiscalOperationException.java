package com.proyecto.proyecto_techlab_springboot.fiscal.exception;

/**
 * Precondiciones de negocio incumplidas
 */
public class InvalidFiscalOperationException extends RuntimeException {
    public InvalidFiscalOperationException(String message) {
        super(message);
    }
}