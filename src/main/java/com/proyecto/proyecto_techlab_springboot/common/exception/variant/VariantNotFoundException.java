package com.proyecto.proyecto_techlab_springboot.common.exception.variant;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class VariantNotFoundException extends ResourceNotFoundException {
    public VariantNotFoundException(String params) {
        super("variante", params);
    }
}
