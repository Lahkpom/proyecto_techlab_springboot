package com.proyecto.proyecto_techlab_springboot.sale.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class SaleNotFoundException extends ResourceNotFoundException {
    public SaleNotFoundException(String params) {
        super("venta", params);
    }
}
