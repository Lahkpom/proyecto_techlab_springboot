package com.proyecto.proyecto_techlab_springboot.common.exception.image;

public class InvalidImageReorderException extends RuntimeException {
    public InvalidImageReorderException(String message) {
        super(message);
    }
}