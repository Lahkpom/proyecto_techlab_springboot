package com.proyecto.proyecto_techlab_springboot.exception.color;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceAlreadyExistsException;

public class ColorAlreadyExistsException extends ResourceAlreadyExistsException {
    public ColorAlreadyExistsException(String params) {
        super("color", params);
    }
}
