package com.proyecto.proyecto_techlab_springboot.exception.image;

public class InvalidImageReorderException extends RuntimeException {
    public InvalidImageReorderException(String message) {
        super(message);
    }
}