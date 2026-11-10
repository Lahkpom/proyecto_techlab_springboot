package com.proyecto.proyecto_techlab_springboot.common.exception.user;

import com.proyecto.proyecto_techlab_springboot.common.exception.common.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String params) {
        super("usuario", params);
    }
}
