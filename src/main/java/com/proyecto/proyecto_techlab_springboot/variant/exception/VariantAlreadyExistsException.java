package com.proyecto.proyecto_techlab_springboot.variant.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceAlreadyExistsException;

public class VariantAlreadyExistsException extends ResourceAlreadyExistsException {
    public VariantAlreadyExistsException(String params) {
        super("variante", params);
    }
}
