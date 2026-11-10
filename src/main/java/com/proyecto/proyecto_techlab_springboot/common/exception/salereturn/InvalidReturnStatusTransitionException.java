package com.proyecto.proyecto_techlab_springboot.common.exception.salereturn;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.InvalidStatusTransitionException;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;

public class InvalidReturnStatusTransitionException extends InvalidStatusTransitionException {

    public InvalidReturnStatusTransitionException(ReturnStatus currentStatus, ReturnStatus targetStatus) {
        super("devolución", currentStatus.toString(), targetStatus.toString());
    }

    public InvalidReturnStatusTransitionException(String message) {
        super(message);
    }
}