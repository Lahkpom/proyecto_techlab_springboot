package com.proyecto.proyecto_techlab_springboot.exception.sale;

import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidStatusTransitionException;

public class InvalidSaleStatusTransitionException extends InvalidStatusTransitionException {
    public InvalidSaleStatusTransitionException(String message) {
        super(message);
    }

    public InvalidSaleStatusTransitionException(String currentStatus, String targetStatus) {
        super("venta", currentStatus, targetStatus);
    }
}