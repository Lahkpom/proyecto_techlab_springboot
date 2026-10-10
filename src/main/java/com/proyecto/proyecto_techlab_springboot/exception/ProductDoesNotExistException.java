package com.proyecto.proyecto_techlab_springboot.exception;

public class ProductDoesNotExistException extends RuntimeException {
    public ProductDoesNotExistException(String message) {
        super("Product does not exist or invalid credentials: " +  message);
    }
}
