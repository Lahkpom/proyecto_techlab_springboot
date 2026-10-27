package com.proyecto.proyecto_techlab_springboot.exception.sale;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class SaleNotFoundException extends ResourceNotFoundException {
    public SaleNotFoundException(String params) {
        super("venta", params);
    }
}
