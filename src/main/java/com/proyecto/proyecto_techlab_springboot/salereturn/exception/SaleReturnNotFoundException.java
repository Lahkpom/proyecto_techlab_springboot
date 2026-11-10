package com.proyecto.proyecto_techlab_springboot.salereturn.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class SaleReturnNotFoundException extends ResourceNotFoundException {
    public SaleReturnNotFoundException(String params) {
        super("devolución", params);
    }
}
