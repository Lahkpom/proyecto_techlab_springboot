package com.proyecto.proyecto_techlab_springboot.exception.fiscal;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class FiscalReceiptNotFoundException extends ResourceNotFoundException {
    public FiscalReceiptNotFoundException(String params) {
        super("documento fiscal", params);
    }
}
