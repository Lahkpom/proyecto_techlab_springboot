package com.proyecto.proyecto_techlab_springboot.salereturn.exception;

public class InvalidReturnRefundException extends RuntimeException {
    public InvalidReturnRefundException(String message) {
        super(message);
    }
}