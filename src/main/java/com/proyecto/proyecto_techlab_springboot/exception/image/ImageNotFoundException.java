package com.proyecto.proyecto_techlab_springboot.exception.image;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class ImageNotFoundException extends ResourceNotFoundException {
    public ImageNotFoundException(String params) {
        super("imagen", params);
    }
}
