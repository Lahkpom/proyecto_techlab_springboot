package com.proyecto.proyecto_techlab_springboot.common.exception.color;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class ColorNotFoundException extends ResourceNotFoundException {
    public ColorNotFoundException(String params) {
        super("color", params);
    }
}
