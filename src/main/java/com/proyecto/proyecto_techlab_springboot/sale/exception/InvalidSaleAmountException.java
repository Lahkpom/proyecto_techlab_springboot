package com.proyecto.proyecto_techlab_springboot.sale.exception;

public class InvalidSaleAmountException extends RuntimeException {
    public InvalidSaleAmountException(String message) {
        super(message);
    }
}