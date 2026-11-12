package com.proyecto.proyecto_techlab_springboot.fiscal.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceAlreadyExistsException;

public class FiscalReceiptAlreadyExistsException extends ResourceAlreadyExistsException {
    public FiscalReceiptAlreadyExistsException(String params) {
        super("comprobante fiscal", params);
    }
}