package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class EmptySaleItemsException extends RuntimeException {

    public EmptySaleItemsException() {
        super("La orden debe contener al menos un producto para iniciar el checkout.");
    }

    public EmptySaleItemsException(String message) {
        super(message);
    }
}