package com.proyecto.proyecto_techlab_springboot.exception.cart;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
