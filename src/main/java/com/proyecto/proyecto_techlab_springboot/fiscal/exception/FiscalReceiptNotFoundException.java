package com.proyecto.proyecto_techlab_springboot.fiscal.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class FiscalReceiptNotFoundException extends ResourceNotFoundException {
    public FiscalReceiptNotFoundException(String params) {
        super("documento fiscal", params);
    }
}
