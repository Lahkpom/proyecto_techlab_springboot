package com.proyecto.proyecto_techlab_springboot.image.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNoLongerAvailableException;

public class ImageCollectionNotAvailableException extends ResourceNoLongerAvailableException {
    public ImageCollectionNotAvailableException(String params) {
        super("galería de imágenes", params);
    }
}
