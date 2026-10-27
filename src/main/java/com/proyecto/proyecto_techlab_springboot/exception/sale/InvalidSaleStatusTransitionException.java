package com.proyecto.proyecto_techlab_springboot.exception.sale;

public class InvalidSaleStatusTransitionException extends RuntimeException {
    public InvalidSaleStatusTransitionException(String message) {
        super(message);
    }

    public InvalidSaleStatusTransitionException(String currentStatus, String targetStatus) {
        super("Transición de estado inválida: no es posible pasar de '%s' a '%s'"
                .formatted(currentStatus, targetStatus)
        );
    }
}