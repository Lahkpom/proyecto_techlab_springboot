package com.proyecto.proyecto_techlab_springboot.exception.user;

import com.proyecto.proyecto_techlab_springboot.exception.common.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String params) {
        super("usuario", params);
    }
}
