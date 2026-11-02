package com.proyecto.proyecto_techlab_springboot.exception.salereturn;

import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;

public class InvalidReturnStatusTransitionException extends InvalidStatusTransitionException {

    public InvalidReturnStatusTransitionException(ReturnStatus currentStatus, ReturnStatus targetStatus) {
        super("devolución", currentStatus.toString(), targetStatus.toString());
    }

    public InvalidReturnStatusTransitionException(String message) {
        super(message);
    }
}