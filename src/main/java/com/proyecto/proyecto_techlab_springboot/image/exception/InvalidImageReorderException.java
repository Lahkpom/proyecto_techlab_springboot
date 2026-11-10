package com.proyecto.proyecto_techlab_springboot.image.exception;

public class InvalidImageReorderException extends RuntimeException {
    public InvalidImageReorderException(String message) {
        super(message);
    }
}