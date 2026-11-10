package com.proyecto.proyecto_techlab_springboot.common.exception.product;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(String params) {
        super("product", params);
    }
}
