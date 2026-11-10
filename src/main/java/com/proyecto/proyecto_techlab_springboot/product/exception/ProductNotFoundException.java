package com.proyecto.proyecto_techlab_springboot.product.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(String params) {
        super("product", params);
    }
}
