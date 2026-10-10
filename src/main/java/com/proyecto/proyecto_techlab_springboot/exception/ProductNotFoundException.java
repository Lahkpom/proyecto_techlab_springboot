package com.proyecto.proyecto_techlab_springboot.exception;

public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(String params) {
        super("product", params);
    }
}
