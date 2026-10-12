package com.proyecto.proyecto_techlab_springboot.exception.color;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class ColorNotFoundException extends ResourceNotFoundException {
    public ColorNotFoundException(String params) {
        super("color", params);
    }
}
