package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidStatusTransitionException;

public class InvalidReturnStatusTransitionException extends InvalidStatusTransitionException {

    public InvalidReturnStatusTransitionException(String currentStatus, String targetStatus) {
        super("devolución", currentStatus, targetStatus);
    }

    public InvalidReturnStatusTransitionException(String message) {
        super(message);
    }
}