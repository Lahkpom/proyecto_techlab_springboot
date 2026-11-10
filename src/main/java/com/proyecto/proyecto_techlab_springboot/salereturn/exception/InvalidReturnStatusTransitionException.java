package com.proyecto.proyecto_techlab_springboot.salereturn.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.InvalidStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;

public class InvalidReturnStatusTransitionException extends InvalidStatusTransitionException {

    public InvalidReturnStatusTransitionException(ReturnStatus currentStatus, ReturnStatus targetStatus) {
        super("devolución", currentStatus.toString(), targetStatus.toString());
    }

    public InvalidReturnStatusTransitionException(String message) {
        super(message);
    }
}