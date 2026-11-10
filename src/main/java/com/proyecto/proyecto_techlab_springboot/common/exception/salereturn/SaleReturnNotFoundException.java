package com.proyecto.proyecto_techlab_springboot.common.exception.salereturn;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class SaleReturnNotFoundException extends ResourceNotFoundException {
    public SaleReturnNotFoundException(String params) {
        super("devolución", params);
    }
}
