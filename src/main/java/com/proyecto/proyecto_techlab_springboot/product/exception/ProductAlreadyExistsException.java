package com.proyecto.proyecto_techlab_springboot.product.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceAlreadyExistsException;

public class ProductAlreadyExistsException extends ResourceAlreadyExistsException {
    public ProductAlreadyExistsException(String params) {
        super("producto", params);
    }
}
