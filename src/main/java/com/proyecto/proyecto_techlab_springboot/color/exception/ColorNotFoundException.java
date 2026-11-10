package com.proyecto.proyecto_techlab_springboot.color.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class ColorNotFoundException extends ResourceNotFoundException {
    public ColorNotFoundException(String params) {
        super("color", params);
    }
}
