package com.proyecto.proyecto_techlab_springboot.exception;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String params) {
        super("usuario", params);
    }
}
