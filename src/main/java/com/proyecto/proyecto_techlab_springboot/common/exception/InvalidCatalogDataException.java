package com.proyecto.proyecto_techlab_springboot.common.exception;

public class InvalidCatalogDataException extends RuntimeException {
    public InvalidCatalogDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
