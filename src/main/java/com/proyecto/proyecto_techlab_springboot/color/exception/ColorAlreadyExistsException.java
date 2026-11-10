package com.proyecto.proyecto_techlab_springboot.color.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceAlreadyExistsException;

public class ColorAlreadyExistsException extends ResourceAlreadyExistsException {
    public ColorAlreadyExistsException(String params) {
        super("color", params);
    }
}
