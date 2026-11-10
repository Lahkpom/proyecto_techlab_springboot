package com.proyecto.proyecto_techlab_springboot.common.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String params) {
        super("Stock insuficiente. " + params);
    }
}
