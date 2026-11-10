package com.proyecto.proyecto_techlab_springboot.common.exception.sale;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class SaleNotFoundException extends ResourceNotFoundException {
    public SaleNotFoundException(String params) {
        super("venta", params);
    }
}
