package com.proyecto.proyecto_techlab_springboot.salereturn.exception;

public class InvalidReturnItemException extends RuntimeException {
    public InvalidReturnItemException(String message) {
        super(message);
    }
}
