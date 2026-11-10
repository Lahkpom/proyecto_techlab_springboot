package com.proyecto.proyecto_techlab_springboot.user.exception;

import com.proyecto.proyecto_techlab_springboot.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String params) {
        super("usuario", params);
    }
}
