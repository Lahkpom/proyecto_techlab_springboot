package com.proyecto.proyecto_techlab_springboot.variant.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class VariantNotFoundException extends ResourceNotFoundException {
    public VariantNotFoundException(String params) {
        super("variante", params);
    }
}
