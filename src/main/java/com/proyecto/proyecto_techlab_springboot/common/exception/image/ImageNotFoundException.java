package com.proyecto.proyecto_techlab_springboot.common.exception.image;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class ImageNotFoundException extends ResourceNotFoundException {
    public ImageNotFoundException(String params) {
        super("imagen", params);
    }
}
