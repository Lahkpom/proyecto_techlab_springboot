package com.proyecto.proyecto_techlab_springboot.common.exception.image;

public class StorageOperationException extends RuntimeException {
    public StorageOperationException(String message) {
        super(message);
    }
}