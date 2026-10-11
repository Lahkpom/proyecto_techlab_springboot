package com.proyecto.proyecto_techlab_springboot.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String params) {
        super("usuario", params);
    }
}
