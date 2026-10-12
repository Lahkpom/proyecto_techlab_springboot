package com.proyecto.proyecto_techlab_springboot.exception.product;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(String params) {
        super("product", params);
    }
}
