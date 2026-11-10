package com.proyecto.proyecto_techlab_springboot.image.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class ImageNotFoundException extends ResourceNotFoundException {
    public ImageNotFoundException(String params) {
        super("imagen", params);
    }
}
