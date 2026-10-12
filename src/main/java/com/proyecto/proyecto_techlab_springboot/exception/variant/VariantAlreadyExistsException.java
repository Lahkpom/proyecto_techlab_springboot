package com.proyecto.proyecto_techlab_springboot.exception.variant;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceAlreadyExistsException;

public class VariantAlreadyExistsException extends ResourceAlreadyExistsException {
    public VariantAlreadyExistsException(String params) {
        super("variante", params);
    }
}
