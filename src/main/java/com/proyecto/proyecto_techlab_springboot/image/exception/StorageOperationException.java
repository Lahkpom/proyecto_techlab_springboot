package com.proyecto.proyecto_techlab_springboot.image.exception;

public class StorageOperationException extends RuntimeException {
    public StorageOperationException(String message) {
        super(message);
    }
}